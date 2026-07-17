package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.ArquivoCreateRequest;
import br.com.achadosperdidos.controller.dto.ArquivoResponse;
import br.com.achadosperdidos.entity.Arquivo;
import br.com.achadosperdidos.entity.Evento;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.repository.ArquivoRepository;
import br.com.achadosperdidos.repository.ClaimRepository;
import br.com.achadosperdidos.repository.ContatoRepository;
import br.com.achadosperdidos.repository.CriancaRepository;
import br.com.achadosperdidos.repository.DevolucaoRepository;
import br.com.achadosperdidos.repository.ItemRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ArquivoService {
    private final ArquivoRepository arquivoRepository;
    private final ItemRepository itemRepository;
    private final ClaimRepository claimRepository;
    private final CriancaRepository criancaRepository;
    private final DevolucaoRepository devolucaoRepository;
    private final ContatoRepository contatoRepository;
    private final SignedResourceIdCodec idCodec;

    /** MIME aceitos no upload (A05/A08): fotos de itens e comprovantes em PDF. */
    private static final Set<String> MIME_PERMITIDOS = Set.of(
            "image/jpeg", "image/pjpeg", "image/png", "image/webp",
            "image/gif", "image/heic", "image/heif", "application/pdf");

    @Value("${app.arquivos.dir}")
    private String arquivosDir;

    public ArquivoService(ArquivoRepository arquivoRepository, ItemRepository itemRepository,
                          ClaimRepository claimRepository, CriancaRepository criancaRepository,
                          DevolucaoRepository devolucaoRepository, ContatoRepository contatoRepository,
                          SignedResourceIdCodec idCodec) {
        this.arquivoRepository = arquivoRepository;
        this.itemRepository = itemRepository;
        this.claimRepository = claimRepository;
        this.criancaRepository = criancaRepository;
        this.devolucaoRepository = devolucaoRepository;
        this.contatoRepository = contatoRepository;
        this.idCodec = idCodec;
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
        // Rejeita caminhos absolutos ou com traversal antes de persistir o metadado.
        a.setNmPath(validarCaminhoRelativo(request.nmPath()));
        a.setTpMime(request.tpMime());
        a.setFgPrincipal(Boolean.TRUE.equals(request.fgPrincipal()));
        a.setQtBytes(request.qtBytes());
        a.setDtCadastro(LocalDateTime.now());
        a.setFgAtivo(true);
        a.setFgExcluido(false);
        return toResponse(arquivoRepository.save(a));
    }

    /**
     * Upload binário: grava o arquivo em disco (app.arquivos.dir) e registra o metadado.
     * Usado pela coleta de itens para anexar fotos (TP_Entidade=ITEM, TP_Arquivo=FOTO).
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
        // caminho relativo por entidade: <TP_Entidade>/<id>/<uuid.ext>
        String relPath = tipo + "/" + idEnt + "/" + nomeFisico;
        try {
            Path destino = resolverDentroDaBase(relPath);
            Files.createDirectories(destino.getParent());
            file.transferTo(destino);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gravar o arquivo em disco.", e);
        }

        Arquivo a = new Arquivo();
        a.setEvento(evento);
        a.setTpEntidade(tipo);
        a.setIdEntidade(idEnt);
        a.setTpArquivo(tpArquivo != null && !tpArquivo.isBlank() ? tpArquivo.trim().toUpperCase() : "FOTO");
        a.setNmArquivo(file.getOriginalFilename() != null ? file.getOriginalFilename() : nomeFisico);
        a.setNmPath(relPath);
        a.setTpMime(file.getContentType());
        a.setFgPrincipal(Boolean.TRUE.equals(fgPrincipal));
        a.setQtBytes(file.getSize());
        a.setDtCadastro(LocalDateTime.now());
        a.setFgAtivo(true);
        a.setFgExcluido(false);
        return toResponse(arquivoRepository.save(a));
    }

    /** Localiza o binário em disco a partir do id assinado do arquivo. */
    @Transactional(readOnly = true)
    public ArquivoConteudo carregarConteudo(String idArquivo) {
        Arquivo a = arquivoRepository.findById(idCodec.decodeArquivoId(idArquivo))
                .filter(x -> !Boolean.TRUE.equals(x.getFgExcluido()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Arquivo não encontrado."));
        // Confina a leitura em app.arquivos.dir: impede path traversal / leitura
        // de arquivo arbitrário mesmo que o metadado tenha nmPath adulterado.
        Path caminho = resolverDentroDaBase(a.getNmPath());
        if (!Files.exists(caminho)) {
            throw new RecursoNaoEncontradoException("Conteúdo do arquivo não encontrado em disco.");
        }
        return new ArquivoConteudo(caminho, a.getNmArquivo(), a.getTpMime());
    }

    /**
     * Resolve {@code relPath} SEMPRE dentro de {@code app.arquivos.dir} e recusa
     * qualquer caminho que escape da base (absoluto, {@code ../}, etc.) — barreira
     * central contra path traversal (OWASP A01/A05).
     */
    private Path resolverDentroDaBase(String relPath) {
        Path base = Paths.get(arquivosDir).toAbsolutePath().normalize();
        Path destino = base.resolve(validarCaminhoRelativo(relPath)).normalize();
        if (!destino.startsWith(base)) {
            throw new IllegalArgumentException("Caminho de arquivo inválido.");
        }
        return destino;
    }

    /** Recusa caminhos nulos, absolutos ou com segmentos de traversal ({@code ..}). */
    private static String validarCaminhoRelativo(String relPath) {
        if (relPath == null || relPath.isBlank()) {
            throw new IllegalArgumentException("Caminho de arquivo não informado.");
        }
        String normalizado = relPath.replace('\\', '/').trim();
        if (normalizado.startsWith("/") || normalizado.contains("..") || normalizado.matches("^[A-Za-z]:.*")) {
            throw new IllegalArgumentException("Caminho de arquivo inválido.");
        }
        return normalizado;
    }

    /** Valida o MIME declarado no upload contra a allowlist (A05/A08). */
    private static void validarMime(String contentType) {
        String mime = contentType == null ? "" : contentType.trim().toLowerCase();
        int ponto = mime.indexOf(';'); // descarta parâmetros, ex.: "; charset=..."
        if (ponto >= 0) mime = mime.substring(0, ponto).trim();
        if (!MIME_PERMITIDOS.contains(mime)) {
            throw new IllegalArgumentException(
                    "Tipo de arquivo não permitido. Envie imagem (JPEG, PNG, WEBP, GIF, HEIC) ou PDF.");
        }
    }

    /** Conteúdo físico de um arquivo (caminho + metadados de download). */
    public record ArquivoConteudo(Path caminho, String nmArquivo, String tpMime) {}

    private static String extrair(String nomeOriginal) {
        if (nomeOriginal == null) return "";
        int ponto = nomeOriginal.lastIndexOf('.');
        if (ponto < 0 || ponto == nomeOriginal.length() - 1) return "";
        String ext = nomeOriginal.substring(ponto).toLowerCase();
        return ext.matches("\\.[a-z0-9]{1,8}") ? ext : "";
    }

    /** Deriva o evento a partir da entidade referenciada (arquivo é polimórfico). */
    private Evento resolverEvento(String tpEntidade, Long idEntidade) {
        Evento evento = switch (tpEntidade) {
            case "ITEM" -> itemRepository.findById(idEntidade).map(x -> x.getEvento()).orElse(null);
            case "CLAIM" -> claimRepository.findById(idEntidade).map(x -> x.getEvento()).orElse(null);
            case "CRIANCA" -> criancaRepository.findById(idEntidade).map(x -> x.getEvento()).orElse(null);
            case "DEVOLUCAO" -> devolucaoRepository.findById(idEntidade).map(x -> x.getEvento()).orElse(null);
            case "CONTATO" -> contatoRepository.findById(idEntidade).map(x -> x.getEvento()).orElse(null);
            default -> throw new IllegalArgumentException(
                    "Tipo de entidade inválido para arquivo: " + tpEntidade + ". Use ITEM, CLAIM, CRIANCA, DEVOLUCAO ou CONTATO.");
        };
        if (evento == null) {
            throw new RecursoNaoEncontradoException("Entidade referenciada (" + tpEntidade + ") não encontrada para vincular o arquivo ao evento.");
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
                a.getTpMime(),
                a.getFgPrincipal(),
                a.getQtBytes(),
                a.getDtCadastro());
    }
}
