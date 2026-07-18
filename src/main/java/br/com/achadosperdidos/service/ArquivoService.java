package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.ArquivoCreateRequest;
import br.com.achadosperdidos.controller.dto.ArquivoResponse;
import br.com.achadosperdidos.entity.Arquivo;
import br.com.achadosperdidos.entity.Evento;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.repository.ArquivoRepository;
import br.com.achadosperdidos.repository.ClaimMensagemRepository;
import br.com.achadosperdidos.repository.ClaimRepository;
import br.com.achadosperdidos.repository.ContatoRepository;
import br.com.achadosperdidos.repository.CriancaRepository;
import br.com.achadosperdidos.repository.DevolucaoRepository;
import br.com.achadosperdidos.repository.ItemRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import br.com.achadosperdidos.storage.ArquivoStorage;
import br.com.achadosperdidos.storage.ArquivoStorageProvider;
import br.com.achadosperdidos.storage.ArquivoStorageRouter;
import br.com.achadosperdidos.storage.LocalArquivoStorage;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class ArquivoService {
    private final ArquivoRepository arquivoRepository;
    private final ItemRepository itemRepository;
    private final ClaimRepository claimRepository;
    private final ClaimMensagemRepository claimMensagemRepository;
    private final CriancaRepository criancaRepository;
    private final DevolucaoRepository devolucaoRepository;
    private final ContatoRepository contatoRepository;
    private final SignedResourceIdCodec idCodec;
    private final ArquivoStorageRouter storageRouter;

    private static final Set<String> MIME_PERMITIDOS = Set.of(
            "image/jpeg", "image/pjpeg", "image/png", "image/webp",
            "image/gif", "image/heic", "image/heif", "application/pdf");

    public ArquivoService(ArquivoRepository arquivoRepository, ItemRepository itemRepository,
                          ClaimRepository claimRepository, ClaimMensagemRepository claimMensagemRepository,
                          CriancaRepository criancaRepository,
                          DevolucaoRepository devolucaoRepository, ContatoRepository contatoRepository,
                          SignedResourceIdCodec idCodec, ArquivoStorageRouter storageRouter) {
        this.arquivoRepository = arquivoRepository;
        this.itemRepository = itemRepository;
        this.claimRepository = claimRepository;
        this.claimMensagemRepository = claimMensagemRepository;
        this.criancaRepository = criancaRepository;
        this.devolucaoRepository = devolucaoRepository;
        this.contatoRepository = contatoRepository;
        this.idCodec = idCodec;
        this.storageRouter = storageRouter;
    }

    @Transactional
    public ArquivoResponse create(ArquivoCreateRequest request) {
        Arquivo a = new Arquivo();
        String tpEntidade = request.tpEntidade().trim().toUpperCase();
        Long idEntidade = idCodec.decodeEntidadeId(tpEntidade, request.idEntidade());
        a.setEvento(resolverEvento(tpEntidade, idEntidade));
        a.setTpEntidade(tpEntidade);
        a.setIdEntidade(idEntidade);
        a.setTpArquivo(request.tpArquivo().trim().toUpperCase());
        a.setNmArquivo(request.nmArquivo());
        a.setNmPath(LocalArquivoStorage.validar(request.nmPath()));
        a.setTpStorage(storageRouter.provedorPadrao().name());
        a.setTpMime(request.tpMime());
        a.setFgPrincipal(Boolean.TRUE.equals(request.fgPrincipal()));
        a.setQtBytes(request.qtBytes());
        a.setDtCadastro(LocalDateTime.now());
        a.setFgAtivo(true);
        a.setFgExcluido(false);
        return toResponse(arquivoRepository.save(a));
    }

    /**
     * Upload binário: grava no provedor padrão (LOCAL/S3) e registra metadados.
     * Em falha de persistência, tenta compensar removendo o objeto físico.
     */
    @Transactional
    public ArquivoResponse upload(String tpEntidade, String idEntidade, String tpArquivo,
                                  MultipartFile file, Boolean fgPrincipal) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo não enviado ou vazio.");
        }
        validarMime(file.getContentType());
        String tipo = tpEntidade.trim().toUpperCase();
        Long idEnt = idCodec.decodeEntidadeId(tipo, idEntidade);
        Evento evento = resolverEvento(tipo, idEnt);

        String extensao = extrair(file.getOriginalFilename());
        String nomeFisico = UUID.randomUUID().toString().replace("-", "") + extensao;
        String relPath = tipo + "/" + idEnt + "/" + nomeFisico;

        ArquivoStorage storage = storageRouter.paraEscrita();
        ArquivoStorageProvider provider = storage.provider();
        try (InputStream in = file.getInputStream()) {
            storage.store(relPath, in, file.getSize(), file.getContentType());
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler o arquivo enviado.", e);
        }

        try {
            Arquivo a = new Arquivo();
            a.setEvento(evento);
            a.setTpEntidade(tipo);
            a.setIdEntidade(idEnt);
            a.setTpArquivo(tpArquivo != null && !tpArquivo.isBlank() ? tpArquivo.trim().toUpperCase() : "FOTO");
            a.setNmArquivo(file.getOriginalFilename() != null ? file.getOriginalFilename() : nomeFisico);
            a.setNmPath(relPath);
            a.setTpStorage(provider.name());
            a.setTpMime(file.getContentType());
            a.setFgPrincipal(Boolean.TRUE.equals(fgPrincipal));
            a.setQtBytes(file.getSize());
            a.setDtCadastro(LocalDateTime.now());
            a.setFgAtivo(true);
            a.setFgExcluido(false);
            return toResponse(arquivoRepository.save(a));
        } catch (RuntimeException ex) {
            try {
                storage.delete(relPath);
            } catch (RuntimeException ignored) {
                // melhor esforço — evita deixar órfão quando possível
            }
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public ArquivoConteudo carregarConteudo(String idArquivo) {
        Arquivo a = arquivoRepository.findById(idCodec.decodeArquivoId(idArquivo))
                .filter(x -> !Boolean.TRUE.equals(x.getFgExcluido()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Arquivo não encontrado."));
        try {
            Resource resource = storageRouter.paraLeitura(a.getTpStorage()).open(a.getNmPath());
            return new ArquivoConteudo(resource, a.getNmArquivo(), a.getTpMime(), a.getQtBytes());
        } catch (IllegalArgumentException e) {
            throw new RecursoNaoEncontradoException(e.getMessage());
        }
    }

    /**
     * Download público de foto principal de item do catálogo (portal).
     * Só libera se o arquivo for FOTO de ITEM em status público.
     */
    @Transactional(readOnly = true)
    public ArquivoConteudo carregarConteudoPublicoItem(String idArquivo) {
        Arquivo a = arquivoRepository.findById(idCodec.decodeArquivoIdAssinado(idArquivo))
                .filter(x -> !Boolean.TRUE.equals(x.getFgExcluido()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Arquivo não encontrado."));
        if (!"ITEM".equalsIgnoreCase(a.getTpEntidade()) || !"FOTO".equalsIgnoreCase(a.getTpArquivo())) {
            throw new RecursoNaoEncontradoException("Arquivo não disponível no portal.");
        }
        var item = itemRepository.findById(a.getIdEntidade())
                .filter(i -> !Boolean.TRUE.equals(i.getFgExcluido()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item não encontrado."));
        String status = item.getStatus() != null ? item.getStatus().getNmStatus() : "";
        if (!List.of("Em estoque", "Com pedido de devolucao", "Aguardando retirada").contains(status)) {
            throw new RecursoNaoEncontradoException("Arquivo não disponível no portal.");
        }
        return carregarConteudo(idArquivo);
    }

    /** Foto principal do item (para catálogo público). */
    @Transactional(readOnly = true)
    public Optional<Arquivo> fotoPrincipalItem(Long idItem) {
        return arquivoRepository
                .findByTpEntidadeAndIdEntidadeAndFgExcluidoFalseOrderByDtCadastroDesc("ITEM", idItem)
                .stream()
                .filter(a -> "FOTO".equalsIgnoreCase(a.getTpArquivo()))
                .sorted((a, b) -> Boolean.compare(
                        Boolean.TRUE.equals(b.getFgPrincipal()),
                        Boolean.TRUE.equals(a.getFgPrincipal())))
                .findFirst();
    }

    private static void validarMime(String contentType) {
        String mime = contentType == null ? "" : contentType.trim().toLowerCase();
        int ponto = mime.indexOf(';');
        if (ponto >= 0) mime = mime.substring(0, ponto).trim();
        if (!MIME_PERMITIDOS.contains(mime)) {
            throw new IllegalArgumentException(
                    "Tipo de arquivo não permitido. Envie imagem (JPEG, PNG, WEBP, GIF, HEIC) ou PDF.");
        }
    }

    public record ArquivoConteudo(Resource resource, String nmArquivo, String tpMime, Long qtBytes) {}

    private static String extrair(String nomeOriginal) {
        if (nomeOriginal == null) return "";
        int ponto = nomeOriginal.lastIndexOf('.');
        if (ponto < 0 || ponto == nomeOriginal.length() - 1) return "";
        String ext = nomeOriginal.substring(ponto).toLowerCase();
        return ext.matches("\\.[a-z0-9]{1,8}") ? ext : "";
    }

    private Evento resolverEvento(String tpEntidade, Long idEntidade) {
        Evento evento = switch (tpEntidade) {
            case "ITEM" -> itemRepository.findById(idEntidade).map(x -> x.getEvento()).orElse(null);
            case "CLAIM" -> claimRepository.findById(idEntidade).map(x -> x.getEvento()).orElse(null);
            case "CLAIM_MENSAGEM" -> claimMensagemRepository.findById(idEntidade)
                    .map(x -> x.getClaim() != null ? x.getClaim().getEvento() : null).orElse(null);
            case "CRIANCA" -> criancaRepository.findById(idEntidade).map(x -> x.getEvento()).orElse(null);
            case "DEVOLUCAO" -> devolucaoRepository.findById(idEntidade).map(x -> x.getEvento()).orElse(null);
            case "CONTATO" -> contatoRepository.findById(idEntidade).map(x -> x.getEvento()).orElse(null);
            default -> throw new IllegalArgumentException(
                    "Tipo de entidade inválido para arquivo: " + tpEntidade
                            + ". Use ITEM, CLAIM, CLAIM_MENSAGEM, CRIANCA, DEVOLUCAO ou CONTATO.");
        };
        if (evento == null) {
            throw new RecursoNaoEncontradoException(
                    "Entidade referenciada (" + tpEntidade + ") não encontrada para vincular o arquivo ao evento.");
        }
        return evento;
    }

    @Transactional(readOnly = true)
    public List<ArquivoResponse> findByEntidade(String tpEntidade, String idEntidade) {
        String tipo = tpEntidade.toUpperCase();
        return arquivoRepository.findByTpEntidadeAndIdEntidadeAndFgExcluidoFalseOrderByDtCadastroDesc(
                        tipo, idCodec.decodeEntidadeId(tipo, idEntidade))
                .stream().map(this::toResponse).toList();
    }

    private ArquivoResponse toResponse(Arquivo a) {
        return new ArquivoResponse(
                idCodec.encodeArquivoId(a.getId()),
                idCodec.encodeEventoId(a.getEvento().getId()),
                a.getTpEntidade(),
                idCodec.encodeEntidadeId(a.getTpEntidade(), a.getIdEntidade()),
                a.getTpArquivo(),
                a.getNmArquivo(),
                a.getNmPath(),
                a.getTpStorage() != null ? a.getTpStorage() : "LOCAL",
                a.getTpMime(),
                a.getFgPrincipal(),
                a.getQtBytes(),
                a.getDtCadastro());
    }
}
