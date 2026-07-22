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
import br.com.achadosperdidos.repository.EventoRepository;
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
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final EventoRepository eventoRepository;
    private final SignedResourceIdCodec idCodec;
    private final ArquivoStorageRouter storageRouter;
    private final ImageThumbnailService imageThumbnailService;

    private static final Set<String> MIME_PERMITIDOS = Set.of(
            "image/jpeg", "image/pjpeg", "image/png", "image/webp",
            "image/gif", "image/heic", "image/heif", "application/pdf");
    private static final Set<String> TIPOS_IMAGEM_EVENTO = Set.of("LOGO", "HERO");

    public ArquivoService(ArquivoRepository arquivoRepository, ItemRepository itemRepository,
                          ClaimRepository claimRepository, ClaimMensagemRepository claimMensagemRepository,
                          CriancaRepository criancaRepository,
                          DevolucaoRepository devolucaoRepository, ContatoRepository contatoRepository,
                          EventoRepository eventoRepository,
                          SignedResourceIdCodec idCodec, ArquivoStorageRouter storageRouter,
                          ImageThumbnailService imageThumbnailService) {
        this.arquivoRepository = arquivoRepository;
        this.itemRepository = itemRepository;
        this.claimRepository = claimRepository;
        this.claimMensagemRepository = claimMensagemRepository;
        this.criancaRepository = criancaRepository;
        this.devolucaoRepository = devolucaoRepository;
        this.contatoRepository = contatoRepository;
        this.eventoRepository = eventoRepository;
        this.idCodec = idCodec;
        this.storageRouter = storageRouter;
        this.imageThumbnailService = imageThumbnailService;
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
        // Miniatura persistida para listagens (portal/painel) — falha não impede o upload.
        gerarEPersistirThumb(storage, relPath, file.getContentType(), file.getSize(),
                file.getOriginalFilename() != null ? file.getOriginalFilename() : nomeFisico);

        try {
            String tpArq = tpArquivo != null && !tpArquivo.isBlank() ? tpArquivo.trim().toUpperCase() : "FOTO";
            if ("EVENTO".equals(tipo) && TIPOS_IMAGEM_EVENTO.contains(tpArq)) {
                invalidarArquivosEventoAnteriores(idEnt, tpArq);
            }
            Arquivo a = new Arquivo();
            a.setEvento(evento);
            a.setTpEntidade(tipo);
            a.setIdEntidade(idEnt);
            a.setTpArquivo(tpArq);
            a.setNmArquivo(file.getOriginalFilename() != null ? file.getOriginalFilename() : nomeFisico);
            a.setNmPath(relPath);
            a.setTpStorage(provider.name());
            a.setTpMime(file.getContentType());
            a.setFgPrincipal(Boolean.TRUE.equals(fgPrincipal) || TIPOS_IMAGEM_EVENTO.contains(tpArq));
            a.setQtBytes(file.getSize());
            a.setDtCadastro(LocalDateTime.now());
            a.setFgAtivo(true);
            a.setFgExcluido(false);
            return toResponse(arquivoRepository.save(a));
        } catch (RuntimeException ex) {
            try {
                storage.delete(relPath);
                storage.delete(thumbKey(relPath));
            } catch (RuntimeException ignored) {
                // melhor esforço — evita deixar órfão quando possível
            }
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public ArquivoConteudo carregarConteudo(String idArquivo) {
        return abrirOriginal(findArquivoAtivo(idArquivo));
    }

    /**
     * Miniatura para listagens: serve arquivo {@code *.thumb.jpg} se existir;
     * senão gera on-the-fly e tenta persistir (backfill de fotos antigas).
     */
    @Transactional(readOnly = true)
    public ArquivoConteudo carregarThumbnail(String idArquivo, Integer maxEdge) {
        return carregarThumbnailDe(findArquivoAtivo(idArquivo), maxEdge);
    }

    /**
     * Download público (portal): foto de item do catálogo OU logo/hero do evento.
     */
    @Transactional(readOnly = true)
    public ArquivoConteudo carregarConteudoPublicoItem(String idArquivo) {
        return abrirOriginal(validarArquivoPublico(idArquivo));
    }

    /** Miniatura pública (mesma regra de acesso do original). */
    @Transactional(readOnly = true)
    public ArquivoConteudo carregarThumbnailPublicoItem(String idArquivo, Integer maxEdge) {
        return carregarThumbnailDe(validarArquivoPublico(idArquivo), maxEdge);
    }

    /** Logo (ou hero) ativo do evento — metadados em `arquivo`, binário no S3/LOCAL. */
    @Transactional(readOnly = true)
    public Optional<Arquivo> imagemEvento(Long idEvento, String tpArquivo) {
        if (idEvento == null || tpArquivo == null) return Optional.empty();
        return arquivoRepository
                .findByTpEntidadeAndIdEntidadeAndFgExcluidoFalseOrderByDtCadastroDesc("EVENTO", idEvento)
                .stream()
                .filter(a -> tpArquivo.equalsIgnoreCase(a.getTpArquivo()))
                .findFirst();
    }

    /** Mapa eventoId → (logo, hero) para listagens sem N+1. */
    @Transactional(readOnly = true)
    public Map<Long, Map<String, Arquivo>> imagensPorEventos(Collection<Long> idEventos) {
        if (idEventos == null || idEventos.isEmpty()) return Map.of();
        Map<Long, Map<String, Arquivo>> out = new HashMap<>();
        for (Arquivo a : arquivoRepository.findByTpEntidadeAndIdEntidadeInAndFgExcluidoFalse("EVENTO", idEventos)) {
            if (!TIPOS_IMAGEM_EVENTO.contains(a.getTpArquivo() == null ? "" : a.getTpArquivo().toUpperCase())) {
                continue;
            }
            out.computeIfAbsent(a.getIdEntidade(), k -> new HashMap<>())
                    .putIfAbsent(a.getTpArquivo().toUpperCase(), a);
        }
        return out;
    }

    private void invalidarArquivosEventoAnteriores(Long idEvento, String tpArquivo) {
        LocalDateTime agora = LocalDateTime.now();
        for (Arquivo old : arquivoRepository
                .findByTpEntidadeAndIdEntidadeAndFgExcluidoFalseOrderByDtCadastroDesc("EVENTO", idEvento)) {
            if (tpArquivo.equalsIgnoreCase(old.getTpArquivo())) {
                old.setFgExcluido(true);
                old.setFgAtivo(false);
                old.setDtAlteracao(agora);
                arquivoRepository.save(old);
            }
        }
    }

    /** Foto principal do item (para catálogo público). */
    @Transactional(readOnly = true)
    public Optional<Arquivo> fotoPrincipalItem(Long idItem) {
        return fotosItem(idItem).stream().findFirst();
    }

    /** Foto principal de vários itens (evita N+1 no catálogo). */
    @Transactional(readOnly = true)
    public Map<Long, Arquivo> fotosPrincipaisPorItens(Collection<Long> idItens) {
        if (idItens == null || idItens.isEmpty()) {
            return Map.of();
        }
        Map<Long, Arquivo> mapa = new HashMap<>();
        for (Arquivo a : arquivoRepository.findByTpEntidadeAndIdEntidadeInAndFgExcluidoFalse("ITEM", idItens)) {
            if (!"FOTO".equalsIgnoreCase(a.getTpArquivo())) continue;
            Arquivo atual = mapa.get(a.getIdEntidade());
            if (atual == null) {
                mapa.put(a.getIdEntidade(), a);
                continue;
            }
            boolean novoPrincipal = Boolean.TRUE.equals(a.getFgPrincipal());
            boolean atualPrincipal = Boolean.TRUE.equals(atual.getFgPrincipal());
            if (novoPrincipal && !atualPrincipal) {
                mapa.put(a.getIdEntidade(), a);
            } else if (novoPrincipal == atualPrincipal
                    && a.getDtCadastro() != null
                    && (atual.getDtCadastro() == null || a.getDtCadastro().isAfter(atual.getDtCadastro()))) {
                mapa.put(a.getIdEntidade(), a);
            }
        }
        return mapa;
    }

    /** Todas as fotos do item (principal primeiro). */
    @Transactional(readOnly = true)
    public List<Arquivo> fotosItem(Long idItem) {
        return arquivoRepository
                .findByTpEntidadeAndIdEntidadeAndFgExcluidoFalseOrderByDtCadastroDesc("ITEM", idItem)
                .stream()
                .filter(a -> "FOTO".equalsIgnoreCase(a.getTpArquivo()))
                .sorted(Comparator
                        .comparing((Arquivo a) -> Boolean.TRUE.equals(a.getFgPrincipal())).reversed()
                        .thenComparing(Arquivo::getDtCadastro, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private Arquivo findArquivoAtivo(String idArquivo) {
        return arquivoRepository.findById(idCodec.decodeArquivoId(idArquivo))
                .filter(x -> !Boolean.TRUE.equals(x.getFgExcluido()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Arquivo não encontrado."));
    }

    private Arquivo validarArquivoPublico(String idArquivo) {
        Arquivo a = arquivoRepository.findById(idCodec.decodeArquivoIdAssinado(idArquivo))
                .filter(x -> !Boolean.TRUE.equals(x.getFgExcluido()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Arquivo não encontrado."));
        if ("EVENTO".equalsIgnoreCase(a.getTpEntidade())
                && TIPOS_IMAGEM_EVENTO.contains(a.getTpArquivo() == null ? "" : a.getTpArquivo().toUpperCase())) {
            eventoRepository.findById(a.getIdEntidade())
                    .filter(e -> !Boolean.TRUE.equals(e.getFgExcluido()) && Boolean.TRUE.equals(e.getFgAtivo()))
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Arquivo não disponível no portal."));
            return a;
        }
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
        return a;
    }

    private ArquivoConteudo abrirOriginal(Arquivo a) {
        try {
            Resource resource = storageRouter.paraLeitura(a.getTpStorage()).open(a.getNmPath());
            return new ArquivoConteudo(resource, a.getNmArquivo(), a.getTpMime(), a.getQtBytes());
        } catch (IllegalArgumentException e) {
            throw new RecursoNaoEncontradoException(e.getMessage());
        }
    }

    private ArquivoConteudo carregarThumbnailDe(Arquivo a, Integer maxEdge) {
        int edge = maxEdge != null ? maxEdge : ImageThumbnailService.DEFAULT_MAX_EDGE;
        ArquivoStorage storage = storageRouter.paraLeitura(a.getTpStorage());
        String keyThumb = thumbKey(a.getNmPath());
        if (storage.exists(keyThumb)) {
            try {
                Resource resource = storage.open(keyThumb);
                return new ArquivoConteudo(resource, nomeThumb(a.getNmArquivo()), "image/jpeg", null);
            } catch (IllegalArgumentException ignored) {
                // cai no fallback on-the-fly
            }
        }
        ArquivoConteudo original = abrirOriginal(a);
        ArquivoConteudo gerada = imageThumbnailService.gerar(original, edge);
        if (isThumbGerada(gerada)) {
            persistirThumbBestEffort(storage, keyThumb, gerada);
        }
        return gerada;
    }

    private void gerarEPersistirThumb(ArquivoStorage storage, String relPath, String contentType,
                                      long size, String nmArquivo) {
        if (!ehImagem(contentType)) return;
        try {
            Resource resource = storage.open(relPath);
            ArquivoConteudo original = new ArquivoConteudo(resource, nmArquivo, contentType, size);
            ArquivoConteudo gerada = imageThumbnailService.gerar(original, ImageThumbnailService.DEFAULT_MAX_EDGE);
            if (isThumbGerada(gerada)) {
                persistirThumbBestEffort(storage, thumbKey(relPath), gerada);
            }
        } catch (RuntimeException ignored) {
            // upload principal já ok
        }
    }

    private static void persistirThumbBestEffort(ArquivoStorage storage, String keyThumb, ArquivoConteudo thumb) {
        try (InputStream in = thumb.resource().getInputStream()) {
            long len = thumb.qtBytes() != null ? thumb.qtBytes() : -1;
            storage.store(keyThumb, in, len, "image/jpeg");
        } catch (Exception ignored) {
            // listagem continua via on-the-fly
        }
    }

    private static boolean isThumbGerada(ArquivoConteudo c) {
        return c != null && c.tpMime() != null && c.tpMime().toLowerCase().startsWith("image/jpeg")
                && c.nmArquivo() != null && c.nmArquivo().endsWith("-thumb.jpg");
    }

    private static boolean ehImagem(String contentType) {
        if (contentType == null) return false;
        String mime = contentType.trim().toLowerCase();
        int ponto = mime.indexOf(';');
        if (ponto >= 0) mime = mime.substring(0, ponto).trim();
        return mime.startsWith("image/") && !mime.contains("pdf");
    }

    /** Chave física da miniatura: {@code ITEM/1/abc.jpg} → {@code ITEM/1/abc.thumb.jpg}. */
    static String thumbKey(String nmPath) {
        String path = LocalArquivoStorage.validar(nmPath);
        int ponto = path.lastIndexOf('.');
        int barra = path.lastIndexOf('/');
        if (ponto > barra) {
            return path.substring(0, ponto) + ".thumb.jpg";
        }
        return path + ".thumb.jpg";
    }

    private static String nomeThumb(String nmArquivo) {
        if (nmArquivo == null || nmArquivo.isBlank()) return "thumb.jpg";
        int ponto = nmArquivo.lastIndexOf('.');
        String base = ponto > 0 ? nmArquivo.substring(0, ponto) : nmArquivo;
        return base + "-thumb.jpg";
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
            case "EVENTO" -> eventoRepository.findById(idEntidade)
                    .filter(e -> !Boolean.TRUE.equals(e.getFgExcluido())).orElse(null);
            case "ITEM" -> itemRepository.findById(idEntidade).map(x -> x.getEvento()).orElse(null);
            case "CLAIM" -> claimRepository.findById(idEntidade).map(x -> x.getEvento()).orElse(null);
            case "CLAIM_MENSAGEM" -> claimMensagemRepository.findById(idEntidade)
                    .map(x -> x.getClaim() != null ? x.getClaim().getEvento() : null).orElse(null);
            case "CRIANCA" -> criancaRepository.findById(idEntidade).map(x -> x.getEvento()).orElse(null);
            case "DEVOLUCAO" -> devolucaoRepository.findById(idEntidade).map(x -> x.getEvento()).orElse(null);
            case "CONTATO" -> contatoRepository.findById(idEntidade).map(x -> x.getEvento()).orElse(null);
            default -> throw new IllegalArgumentException(
                    "Tipo de entidade inválido para arquivo: " + tpEntidade
                            + ". Use EVENTO, ITEM, CLAIM, CLAIM_MENSAGEM, CRIANCA, DEVOLUCAO ou CONTATO.");
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
