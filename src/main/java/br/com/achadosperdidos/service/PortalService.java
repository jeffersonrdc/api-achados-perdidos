package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.*;
import br.com.achadosperdidos.entity.*;
import br.com.achadosperdidos.exception.EmailEmUsoException;
import br.com.achadosperdidos.exception.PortalIndisponivelException;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.pagination.PaginationMeta;
import br.com.achadosperdidos.pagination.PaginationParams;
import br.com.achadosperdidos.repository.*;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class PortalService {

    /** Status que tornam o item visível na consulta pública (após triagem, no estoque). */
    private static final List<String> STATUS_PORTAL = List.of(
            "Em estoque", "Com pedido de devolucao", "Aguardando retirada");
    /** Pedido de retirada em andamento: item sai do catálogo mesmo se continuar Em estoque. */
    private static final List<String> CLAIM_STATUS_OCULTA_PORTAL = List.of(
            "Claim Aberto", "Claim em Análise");
    private static final int MAX_COMPROVANTES_RETIRADA = 5;
    private static final long MAX_BYTES_COMPROVANTE = 10L * 1024 * 1024;
    private static final Set<String> MIME_COMPROVANTE = Set.of(
            "image/jpeg", "image/pjpeg", "image/png", "application/pdf");

    private final EventoRepository eventoRepository;
    private final EventoConfiguracaoRepository eventoConfiguracaoRepository;
    private final ItemRepository itemRepository;
    private final DevolucaoRepository devolucaoRepository;
    private final ClaimRepository claimRepository;
    private final ClaimValidacaoRepository claimValidacaoRepository;
    private final ArquivoRepository arquivoRepository;
    private final TriagemRepository triagemRepository;
    private final ClaimService claimService;
    private final MatchService matchService;
    private final ArquivoService arquivoService;
    private final CategoriaService categoriaService;
    private final TagService tagService;
    private final LocalService localService;
    private final StatusItemService statusItemService;
    private final CriancaService criancaService;
    private final CatalogoService catalogoService;
    private final EstadoService estadoService;
    private final EmailService emailService;
    private final PortalContatosConfigService portalContatosConfigService;
    private final PerfilRepository perfilRepository;
    private final UsuarioRepository usuarioRepository;
    private final WallpaperDownloadRepository wallpaperDownloadRepository;
    private final PasswordEncoder passwordEncoder;
    private final SignedResourceIdCodec idCodec;

    @PersistenceContext
    private EntityManager em;

    public PortalService(EventoRepository eventoRepository,
                         EventoConfiguracaoRepository eventoConfiguracaoRepository,
                         ItemRepository itemRepository,
                         DevolucaoRepository devolucaoRepository,
                         ClaimRepository claimRepository,
                         ClaimValidacaoRepository claimValidacaoRepository,
                         ArquivoRepository arquivoRepository,
                         TriagemRepository triagemRepository,
                         ClaimService claimService,
                         MatchService matchService,
                         ArquivoService arquivoService,
                         CategoriaService categoriaService,
                         TagService tagService,
                         LocalService localService,
                         StatusItemService statusItemService,
                         CriancaService criancaService,
                         CatalogoService catalogoService,
                         EstadoService estadoService,
                         EmailService emailService,
                         PortalContatosConfigService portalContatosConfigService,
                         PerfilRepository perfilRepository,
                         UsuarioRepository usuarioRepository,
                         WallpaperDownloadRepository wallpaperDownloadRepository,
                         PasswordEncoder passwordEncoder,
                         SignedResourceIdCodec idCodec) {
        this.eventoRepository = eventoRepository;
        this.eventoConfiguracaoRepository = eventoConfiguracaoRepository;
        this.itemRepository = itemRepository;
        this.devolucaoRepository = devolucaoRepository;
        this.claimRepository = claimRepository;
        this.claimValidacaoRepository = claimValidacaoRepository;
        this.arquivoRepository = arquivoRepository;
        this.triagemRepository = triagemRepository;
        this.claimService = claimService;
        this.matchService = matchService;
        this.arquivoService = arquivoService;
        this.categoriaService = categoriaService;
        this.tagService = tagService;
        this.localService = localService;
        this.statusItemService = statusItemService;
        this.criancaService = criancaService;
        this.catalogoService = catalogoService;
        this.estadoService = estadoService;
        this.emailService = emailService;
        this.portalContatosConfigService = portalContatosConfigService;
        this.perfilRepository = perfilRepository;
        this.usuarioRepository = usuarioRepository;
        this.wallpaperDownloadRepository = wallpaperDownloadRepository;
        this.passwordEncoder = passwordEncoder;
        this.idCodec = idCodec;
    }

    @Transactional(readOnly = true)
    public List<PortalEventoResumoResponse> listarEventosAbertos() {
        List<Evento> eventos = eventoRepository.findByFgExcluidoFalseAndFgAtivoTrueOrderByDtInicioDesc();
        Map<Long, Map<String, Arquivo>> imgs = arquivoService.imagensPorEventos(
                eventos.stream().map(Evento::getId).toList());
        return eventos.stream()
                .map(e -> toEventoResumo(e, imgs.getOrDefault(e.getId(), Map.of())))
                .filter(e -> Boolean.TRUE.equals(e.fgConsultaPublica()) || Boolean.TRUE.equals(e.fgAceitaClaim()))
                .toList();
    }

    /**
     * KPIs agregados dos eventos com portal habilitado (cards de /como-funciona).
     * Taxa = devoluções concluídas / itens registrados; tempo médio = cadastro do item → devolução.
     */
    @Transactional(readOnly = true)
    public PortalMetricasResponse metricasPublicas() {
        List<Long> ids = idsEventosPortal();
        if (ids.isEmpty()) {
            return new PortalMetricasResponse(0, 0, 0, 0);
        }
        long registrados = itemRepository.countAtivosByEventoIds(ids);
        long devolvidos = devolucaoRepository.countConcluidasByEventoIds(ids);
        int taxa = registrados == 0 ? 0 : (int) Math.min(100, Math.round(100.0 * devolvidos / registrados));
        int horas = (int) Math.round(Math.max(0, avgHorasResolucao(ids)));
        return new PortalMetricasResponse(registrados, devolvidos, taxa, horas);
    }

    private List<Long> idsEventosPortal() {
        return eventoRepository.findByFgExcluidoFalseAndFgAtivoTrueOrderByDtInicioDesc().stream()
                .filter(e -> {
                    EventoConfiguracao cfg = eventoConfiguracaoRepository
                            .findByEvento_IdAndFgExcluidoFalse(e.getId())
                            .orElseGet(() -> configPadrao(e));
                    return Boolean.TRUE.equals(cfg.getFgConsultaPublica())
                            || Boolean.TRUE.equals(cfg.getFgAceitaClaim());
                })
                .map(Evento::getId)
                .toList();
    }

    private double avgHorasResolucao(List<Long> ids) {
        Object raw = em.createNativeQuery(
                        "SELECT AVG(TIMESTAMPDIFF(MINUTE, i.DT_Cadastro, d.DT_Devolucao) / 60.0) " +
                                "FROM devolucao d JOIN item i ON i.ID_Item = d.IDR_Item " +
                                "WHERE d.FG_Excluido = 0 AND d.FG_Concluido = 1 " +
                                "AND d.IDR_Evento IN (:ids) " +
                                "AND i.DT_Cadastro IS NOT NULL AND d.DT_Devolucao IS NOT NULL " +
                                "AND d.DT_Devolucao >= i.DT_Cadastro")
                .setParameter("ids", ids)
                .getSingleResult();
        if (raw == null) return 0;
        return ((Number) raw).doubleValue();
    }

    /**
     * Formulário público /contato: envia e-mail para o remetente da conta SMTP
     * vinculada ao parâmetro PORTAL_CONTATO (Configurações → E-mail / SMTP).
     */
    public PortalContatoResponse enviarContato(PortalContatoRequest request) {
        String nome = request.nmNome().trim();
        String email = request.nmEmail().trim().toLowerCase();
        String mensagem = request.dsMensagem().trim();
        if (mensagem.isEmpty()) {
            throw new IllegalArgumentException("Informe a mensagem.");
        }
        String protocolo = "CT-" + ThreadLocalRandom.current().nextInt(10000, 100000);
        String assunto = rotuloAssuntoContato(request.nmAssunto());

        Map<String, String> vars = new HashMap<>();
        vars.put("nome", nome);
        vars.put("email", email);
        vars.put("assunto", assunto);
        vars.put("mensagem", mensagem);
        vars.put("protocolo", protocolo);
        vars.put("ano", String.valueOf(Year.now().getValue()));

        EmailService.Resultado resultado = emailService.enviarParaRemetenteConfigurado(
                "PORTAL_CONTATO", email, vars);
        if (!resultado.enviado()) {
            throw new IllegalArgumentException(resultado.erro() != null
                    ? resultado.erro()
                    : "Não foi possível enviar a mensagem. Tente novamente em instantes.");
        }
        return new PortalContatoResponse(protocolo, "Mensagem enviada com sucesso.");
    }

    @Transactional(readOnly = true)
    public PortalContatosConfigResponse contatosPortal() {
        return portalContatosConfigService.obter();
    }

    @Transactional(readOnly = true)
    public List<PortalWallpaperResponse> listarWallpapers(String idEvento) {
        Evento e = findEvento(idCodec.decodeEventoIdAssinado(idEvento));
        return arquivoService.listarWallpapersEvento(e.getId()).stream()
                .map(a -> {
                    String id = idCodec.encodeArquivoId(a.getId());
                    return new PortalWallpaperResponse(
                            id,
                            a.getNmArquivo(),
                            "/api/v1/portal/arquivos/" + id + "/download",
                            "/api/v1/portal/arquivos/" + id + "/thumbnail?max=600");
                })
                .toList();
    }

    /**
     * Registra um download de wallpaper feito no portal (/wallpaper) e devolve o
     * total acumulado do evento — alimenta o card "Wallpapers Baixados" do painel.
     */
    @Transactional
    public PortalWallpaperDownloadResponse registrarDownloadWallpaper(
            String idEvento, PortalWallpaperDownloadRequest request, String ip, String userAgent) {
        Evento evento = findEvento(idCodec.decodeEventoIdAssinado(idEvento));

        WallpaperDownload download = new WallpaperDownload();
        download.setEvento(evento);
        download.setArquivo(resolverArquivoWallpaper(evento, request));
        download.setNmOrigem("PORTAL");
        download.setNrIp(ip);
        download.setDsUserAgent(truncar(userAgent, 300));
        download.setDtDownload(LocalDateTime.now());
        download.setDtCadastro(LocalDateTime.now());
        download.setFgAtivo(true);
        download.setFgExcluido(false);
        wallpaperDownloadRepository.save(download);

        return new PortalWallpaperDownloadResponse(
                wallpaperDownloadRepository.contarPorEvento(evento.getId(), null));
    }

    /** Arte escolhida; ignora IDs inválidos ou de outro evento para não perder a contagem. */
    private Arquivo resolverArquivoWallpaper(Evento evento, PortalWallpaperDownloadRequest request) {
        String idArquivo = request != null ? request.idArquivo() : null;
        if (idArquivo == null || idArquivo.isBlank()) return null;
        try {
            return arquivoRepository.findById(idCodec.decodeArquivoId(idArquivo))
                    .filter(a -> !Boolean.TRUE.equals(a.getFgExcluido()))
                    .filter(a -> a.getEvento() != null && evento.getId().equals(a.getEvento().getId()))
                    .orElse(null);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static String truncar(String valor, int max) {
        if (valor == null) return null;
        String v = valor.trim();
        return v.length() <= max ? v : v.substring(0, max);
    }

    private static String rotuloAssuntoContato(String codigo) {
        if (codigo == null || codigo.isBlank()) return "Sem assunto";
        return switch (codigo.trim()) {
            case "item-perdido" -> "Dúvida sobre item perdido";
            case "item-achado" -> "Dúvida sobre item achado";
            case "retirada" -> "Problema com retirada";
            case "protocolo" -> "Consulta de protocolo";
            case "elogio" -> "Elogio / feedback";
            case "outro" -> "Outro assunto";
            default -> codigo.trim();
        };
    }

    /**
     * Indica se o portal público já está liberado (agora ≥ dtInicio e ≤ dtFim, evento ativo).
     * Usado pela splash do portal após a vinheta.
     */
    @Transactional(readOnly = true)
    public PortalStatusResponse statusPortal() {
        Evento evento = eventoRepository.findByFgExcluidoFalseAndFgAtivoTrueOrderByDtInicioDesc().stream()
                .filter(e -> {
                    EventoConfiguracao cfg = eventoConfiguracaoRepository
                            .findByEvento_IdAndFgExcluidoFalse(e.getId())
                            .orElseGet(() -> configPadrao(e));
                    return Boolean.TRUE.equals(cfg.getFgConsultaPublica())
                            || Boolean.TRUE.equals(cfg.getFgAceitaClaim());
                })
                .findFirst()
                .orElse(null);
        if (evento == null) {
            return new PortalStatusResponse(
                    false, null, null, null, null,
                    "Nenhum evento disponível no momento. Volte em breve.");
        }
        LocalDateTime agora = LocalDateTime.now();
        boolean antes = evento.getDtInicio() != null && agora.isBefore(evento.getDtInicio());
        boolean depois = evento.getDtFim() != null && agora.isAfter(evento.getDtFim());
        boolean liberado = !antes && !depois;
        String mensagem;
        if (antes) {
            mensagem = "O portal de Achados e Perdidos abre em "
                    + formatarDataHora(evento.getDtInicio())
                    + ". Enquanto isso, aproveite o festival — estamos nos preparando para te ajudar.";
        } else if (depois) {
            mensagem = "O período de consulta pública deste evento foi encerrado em "
                    + formatarDataHora(evento.getDtFim()) + ".";
        } else {
            mensagem = "Portal liberado para consulta e registro.";
        }
        return new PortalStatusResponse(
                liberado,
                idCodec.encodeEventoId(evento.getId()),
                evento.getNmEvento(),
                evento.getDtInicio(),
                evento.getDtFim(),
                mensagem);
    }

    private static String formatarDataHora(LocalDateTime dt) {
        if (dt == null) return "";
        return dt.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm"));
    }

    @Transactional(readOnly = true)
    public PortalEventoResumoResponse detalharEvento(String idEvento) {
        Evento e = findEvento(idCodec.decodeEventoIdAssinado(idEvento));
        exigirJanelaPortal(e);
        Map<String, Arquivo> imgs = arquivoService.imagensPorEventos(List.of(e.getId()))
                .getOrDefault(e.getId(), Map.of());
        return toEventoResumo(e, imgs);
    }

    /** Categorias para os formulários públicos (registro de objeto perdido). */
    @Transactional(readOnly = true)
    public List<br.com.achadosperdidos.controller.dto.CategoriaResponse> listarCategorias() {
        return categoriaService.findAll();
    }

    /** Subcategorias ativas de uma categoria-pai (portal). */
    @Transactional(readOnly = true)
    public List<br.com.achadosperdidos.controller.dto.CategoriaResponse> listarSubcategorias(String idCategoria) {
        return categoriaService.findSubcategorias(idCategoria, false);
    }

    /** Tags ativas de uma subcategoria (portal). */
    @Transactional(readOnly = true)
    public List<br.com.achadosperdidos.controller.dto.TagResponse> listarTags(String idSubcategoria) {
        return tagService.findAll(false, idSubcategoria);
    }

    /** Marcas ativas para selects do formulário público (opcionalmente por subcategoria). */
    @Transactional(readOnly = true)
    public List<String> listarMarcas() {
        return catalogoService.listarMarcas(null);
    }

    @Transactional(readOnly = true)
    public List<String> listarMarcas(String subcategoria) {
        return catalogoService.listarMarcas(subcategoria);
    }

    /** Modelos ativos da marca (cascata marca → modelo). */
    @Transactional(readOnly = true)
    public List<String> listarModelos(String marca) {
        return catalogoService.listarModelos(marca, null);
    }

    /** Modelos ativos da marca filtrados pela subcategoria (quando informada). */
    @Transactional(readOnly = true)
    public List<String> listarModelos(String marca, String subcategoria) {
        return catalogoService.listarModelos(marca, subcategoria);
    }

    /** Cores ativas para selects do formulário público. */
    @Transactional(readOnly = true)
    public List<String> listarCores() {
        return catalogoService.listarCores();
    }

    /** Estados do objeto ativos para selects do formulário público. */
    @Transactional(readOnly = true)
    public List<String> listarEstados() {
        return estadoService.listarNomesAtivos();
    }

    /** Locais do evento para os selects de localização (slim, público). */
    @Transactional(readOnly = true)
    public List<br.com.achadosperdidos.controller.dto.PortalLocalResponse> listarLocais(String idEvento) {
        return localService.findByEvento(idEvento).stream()
                .map(l -> new br.com.achadosperdidos.controller.dto.PortalLocalResponse(l.id(), l.nmLocal(), l.tpLocal()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ApiPage<PortalItemCatalogoResponse> catalogoItens(String idEvento, Integer page, Integer limit, String pesquisa) {
        exigirConsultaPublica(idEvento);
        int p = PaginationParams.resolvePage(page);
        int l = PaginationParams.resolveLimit(limit);
        Long eventoId = idCodec.decodeEventoIdAssinado(idEvento);
        // Só aparecem no portal após "Concluir triagem" (triagem CONCLUIDA) e status de estoque/pós-claim.
        // Pedido de retirada com Claim Aberto (ou em Análise) oculta o item, mesmo Em estoque.
        Page<Item> result = itemRepository.findCatalogoPortal(
                eventoId, STATUS_PORTAL, CLAIM_STATUS_OCULTA_PORTAL, PageRequest.of(p - 1, l));
        var filtrados = result.getContent().stream()
                .filter(i -> pesquisa == null || pesquisa.isBlank() || matchesPesquisa(i, pesquisa))
                .toList();
        var fotos = arquivoService.fotosPrincipaisPorItens(filtrados.stream().map(Item::getId).toList());
        var content = filtrados.stream()
                .map(i -> toCatalogoItem(i, fotos.get(i.getId())))
                .toList();
        return ApiPage.paged(content, new PaginationMeta(p, l, result.getTotalElements(), result.getTotalPages()));
    }

    @Transactional
    public ClaimResponse registrarObjetoPerdido(String idEvento, PortalClaimCreateRequest request) {
        exigirAceitaClaim(idEvento);
        Evento evento = findEvento(idCodec.decodeEventoIdAssinado(idEvento));
        Claim claim = new Claim();
        claim.setEvento(evento);
        claim.setTpClaim(ClaimService.TIPO_PERDA);
        claimService.aplicarDadosBasicos(claim,
                request.idCategoria(), request.idSubcategoria(), null,
                request.nmNome(), request.nrCpf(), request.nmEmail(), request.nrTelefone(),
                request.nmObjeto(), request.dsObjeto(), request.nmMarca(), request.nmModelo(),
                request.nmCor(), request.nmEstado(), request.dsTags(), request.tpPrioridade(),
                request.fgSensivel(), request.dtPerdeu(), request.hrPerdeu(),
                request.idLocal(), request.nmLocal());
        claim.setDsWallpaper(blankToNull(request.dsWallpaper()));
        claim.setDtCadastro(LocalDateTime.now());
        claim.setFgAtivo(true);
        claim.setFgExcluido(false);
        aplicarContatoConfianca(claim,
                request.nmContatoConfianca(), request.nrTelefoneConfianca(), request.dsRelacaoContatoConfianca());
        claim = claimRepository.save(claim);
        claim.setCdClaim(claimService.gerarProtocolo(claim.getId(), claim.getDtCadastro()));
        claim = claimRepository.save(claim);
        matchService.recalcularMatches(claim);
        return claimService.toResponse(claimRepository.findById(claim.getId()).orElse(claim));
    }

    @Transactional
    public PortalClaimResultResponse reclamarItem(String idEvento, PortalClaimItemRequest request) {
        exigirAceitaClaim(idEvento);
        Long eventoId = idCodec.decodeEventoIdAssinado(idEvento);
        Item item = itemRepository.findById(idCodec.decodeItemIdAssinado(request.idItem()))
                .filter(i -> !Boolean.TRUE.equals(i.getFgExcluido()))
                .filter(i -> i.getEvento().getId().equals(eventoId))
                .filter(i -> !Boolean.TRUE.equals(i.getFgEntregue()))
                .filter(i -> !Boolean.TRUE.equals(i.getFgDescartado()))
                .filter(i -> i.getStatus() != null && STATUS_PORTAL.contains(i.getStatus().getNmStatus()))
                .filter(this::triagemConcluida)
                .filter(i -> !temRetiradaPendenteNoPortal(i.getId()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item não encontrado no evento."));

        Claim claim = new Claim();
        claim.setEvento(item.getEvento());
        claim.setTpClaim(ClaimService.TIPO_RETIRADA);
        claim.setCategoria(item.getCategoria());
        claim.setSubcategoria(item.getSubcategoria());
        claim.setStatus(statusItemService.findByNomeOrDefault(null, "Claim Aberto"));
        claim.setNmNome(request.nmNome().trim());
        claim.setNrCpf(request.nrCpf());
        claim.setNmEmail(request.nmEmail().trim().toLowerCase());
        claim.setNrTelefone(request.nrTelefone());
        claim.setNmObjeto(item.getNmTitulo());
        String dsObjeto = blankToNull(request.dsObjeto());
        if (dsObjeto == null) {
            dsObjeto = blankToNull(request.dsObservacao());
        }
        claim.setDsObjeto(dsObjeto);
        claim.setDsDetalhesOcultos(blankToNull(request.dsDetalhesOcultos()));
        claim.setNmMarca(item.getNmMarca());
        claim.setNmModelo(item.getNmModelo());
        claim.setNmCor(item.getNmCor());
        claim.setNmEstado(item.getNmEstado());
        claim.setDtPerdeu(item.getDtEncontrado());
        claim.setNmLocal(item.getNmLocalEncontrado());
        claim.setFgSensivel(Boolean.TRUE.equals(item.getFgSensivel()));
        claim.setTpPrioridade(item.getTpPrioridade());
        claim.setDtCadastro(LocalDateTime.now());
        claim.setFgAtivo(true);
        claim.setFgExcluido(false);
        aplicarContatoConfianca(claim,
                request.nmContatoConfianca(), request.nrTelefoneConfianca(), request.dsRelacaoContatoConfianca());
        claim = claimRepository.save(claim);
        claim.setCdClaim(claimService.gerarProtocolo(claim.getId(), claim.getDtCadastro()));
        claim = claimRepository.save(claim);

        ClaimValidacao validacao = new ClaimValidacao();
        validacao.setEvento(item.getEvento());
        validacao.setClaim(claim);
        validacao.setItem(item);
        validacao.setStResultado("PENDENTE");
        validacao.setDtCadastro(LocalDateTime.now());
        validacao.setFgExcluido(false);
        validacao = claimValidacaoRepository.save(validacao);

        return new PortalClaimResultResponse(
                idCodec.encodeClaimId(claim.getId()),
                idCodec.encodeClaimValidacaoId(validacao.getId()),
                validacao.getStResultado(),
                "Claim registrado. A equipe do evento irá validar a correspondência com o item.");
    }

    /**
     * Anexa comprovantes ao pedido público de retirada.
     * O vínculo é feito diretamente no claim para que os arquivos apareçam na análise administrativa.
     */
    @Transactional
    public List<ArquivoResponse> uploadComprovantesRetirada(
            String idEvento, String idClaim, List<MultipartFile> anexos) {
        exigirAceitaClaim(idEvento);
        Long eventoId = idCodec.decodeEventoIdAssinado(idEvento);
        Claim claim = claimRepository.findById(idCodec.decodeClaimIdAssinado(idClaim))
                .filter(c -> !Boolean.TRUE.equals(c.getFgExcluido()))
                .filter(c -> c.getEvento().getId().equals(eventoId))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitação não encontrada no evento."));
        if (!ClaimService.TIPO_RETIRADA.equalsIgnoreCase(claim.getTpClaim())) {
            throw new IllegalArgumentException("Somente solicitações de retirada aceitam comprovantes neste endpoint.");
        }

        List<MultipartFile> arquivos = anexos == null ? List.of() : anexos.stream()
                .filter(f -> f != null && !f.isEmpty())
                .toList();
        if (arquivos.isEmpty()) {
            throw new IllegalArgumentException("Envie ao menos um comprovante.");
        }
        long existentes = arquivoRepository
                .countByTpEntidadeAndIdEntidadeAndTpArquivoIgnoreCaseAndFgExcluidoFalse(
                        "CLAIM", claim.getId(), "COMPROVANTE");
        if (existentes + arquivos.size() > MAX_COMPROVANTES_RETIRADA) {
            throw new IllegalArgumentException(
                    "A solicitação aceita no máximo " + MAX_COMPROVANTES_RETIRADA + " comprovantes.");
        }
        for (MultipartFile arquivo : arquivos) {
            if (arquivo.getSize() > MAX_BYTES_COMPROVANTE) {
                throw new IllegalArgumentException("Cada comprovante deve ter no máximo 10 MB.");
            }
            String mime = normalizarMime(arquivo.getContentType());
            if (!MIME_COMPROVANTE.contains(mime)) {
                throw new IllegalArgumentException("Envie apenas comprovantes PDF, JPEG ou PNG.");
            }
        }

        String claimAssinado = idCodec.encodeClaimId(claim.getId());
        return arquivos.stream()
                .map(arquivo -> arquivoService.upload(
                        "CLAIM", claimAssinado, "COMPROVANTE", arquivo, false))
                .toList();
    }

    /** Upload público de foto para relato de perda (CLAIM / FOTO). */
    @Transactional
    public ArquivoResponse uploadFotoClaim(String idEvento, String idClaim, org.springframework.web.multipart.MultipartFile file) {
        exigirAceitaClaim(idEvento);
        Long eventoId = idCodec.decodeEventoIdAssinado(idEvento);
        Claim claim = claimRepository.findById(idCodec.decodeClaimIdAssinado(idClaim))
                .filter(c -> !Boolean.TRUE.equals(c.getFgExcluido()))
                .filter(c -> c.getEvento().getId().equals(eventoId))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Claim não encontrado no evento."));
        if (!ClaimService.TIPO_PERDA.equalsIgnoreCase(claim.getTpClaim())) {
            throw new IllegalArgumentException("Somente relatos de perda (PERDA) aceitam foto pelo portal.");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo não enviado ou vazio.");
        }
        String mime = file.getContentType() != null ? file.getContentType().toLowerCase() : "";
        if (!mime.equals("image/jpeg") && !mime.equals("image/pjpeg") && !mime.equals("image/png")) {
            throw new IllegalArgumentException("Apenas JPEG ou PNG são aceitos.");
        }
        if (file.getSize() > 5L * 1024 * 1024) {
            throw new IllegalArgumentException("A foto deve ter no máximo 5 MB.");
        }
        return arquivoService.upload("CLAIM", idCodec.encodeClaimId(claim.getId()), "FOTO", file, true);
    }

    private static String normalizarMime(String contentType) {
        if (contentType == null) return "";
        String mime = contentType.trim().toLowerCase();
        int separator = mime.indexOf(';');
        return separator >= 0 ? mime.substring(0, separator).trim() : mime;
    }

    @Transactional(readOnly = true)
    public List<ClaimResponse> meusClaims(String idEvento, String email) {
        exigirAceitaClaim(idEvento);
        return claimRepository.findByNmEmailIgnoreCaseAndEvento_IdAndFgExcluidoFalseOrderByDtCadastroDesc(
                        email.trim().toLowerCase(), idCodec.decodeEventoIdAssinado(idEvento))
                .stream().map(claimService::toResponse).toList();
    }

    @Transactional
    public CriancaResponse cadastrarCrianca(String idEvento, CriancaCreateRequest request) {
        if (!idEvento.equals(request.idEvento())) {
            throw new IllegalArgumentException("O evento do corpo deve coincidir com o evento do portal.");
        }
        findEvento(idCodec.decodeEventoIdAssinado(idEvento));
        return criancaService.create(request);
    }

    @Transactional
    public CriancaResponsavelResponse vincularResponsavel(CriancaResponsavelCreateRequest request) {
        return criancaService.addResponsavel(request);
    }

    @Transactional
    public UsuarioResponse registrarParticipante(PortalParticipanteRegisterRequest request) {
        if (usuarioRepository.findByNmEmail(request.nmEmail().trim().toLowerCase()).isPresent()) {
            throw new EmailEmUsoException("E-mail já cadastrado. Faça login para continuar.");
        }
        Perfil perfil = perfilRepository.findByNmPerfilIgnoreCaseAndFgExcluidoFalse("Participante")
                .orElseGet(() -> criarPerfilParticipante());

        Usuario u = new Usuario();
        u.setPerfil(perfil);
        u.setNmUsuario(request.nmUsuario().trim());
        u.setNmLogin(request.nmEmail().trim().toLowerCase());
        u.setNmEmail(request.nmEmail().trim().toLowerCase());
        u.setNmSenha(passwordEncoder.encode(request.senha()));
        u.setDtCadastro(LocalDateTime.now());
        u.setFgAtivo(true);
        u.setFgExcluido(false);
        u = usuarioRepository.save(u);
        return UsuarioResponse.of(
                idCodec.encodeUsuarioId(u.getId()),
                u.getNmUsuario(),
                u.getNmLogin(),
                u.getNmEmail(),
                u.getPerfil().getNmPerfil(),
                u.getFgAtivo());
    }

    private Perfil criarPerfilParticipante() {
        Perfil p = new Perfil();
        p.setNmPerfil("Participante");
        p.setDsPerfil("Público do evento — consulta e claims");
        p.setDtCadastro(LocalDateTime.now());
        p.setFgAtivo(true);
        p.setFgExcluido(false);
        return perfilRepository.save(p);
    }

    private void exigirConsultaPublica(String idEvento) {
        Evento evento = findEvento(idCodec.decodeEventoIdAssinado(idEvento));
        exigirJanelaPortal(evento);
        EventoConfiguracao cfg = config(idEvento);
        if (!Boolean.TRUE.equals(cfg.getFgConsultaPublica())) {
            throw new PortalIndisponivelException("Consulta pública desabilitada para este evento.");
        }
    }

    private void exigirAceitaClaim(String idEvento) {
        Evento evento = findEvento(idCodec.decodeEventoIdAssinado(idEvento));
        exigirJanelaPortal(evento);
        EventoConfiguracao cfg = config(idEvento);
        if (!Boolean.TRUE.equals(cfg.getFgAceitaClaim())) {
            throw new PortalIndisponivelException("Registro de objetos perdidos desabilitado para este evento.");
        }
    }

    /** Portal só fica utilizável a partir de DT_Inicio até DT_Fim. */
    private void exigirJanelaPortal(Evento evento) {
        LocalDateTime agora = LocalDateTime.now();
        if (evento.getDtInicio() != null && agora.isBefore(evento.getDtInicio())) {
            throw new PortalIndisponivelException(
                    "Portal ainda não liberado. Abre em " + formatarDataHora(evento.getDtInicio()) + ".");
        }
        if (evento.getDtFim() != null && agora.isAfter(evento.getDtFim())) {
            throw new PortalIndisponivelException(
                    "Portal encerrado em " + formatarDataHora(evento.getDtFim()) + ".");
        }
    }

    private EventoConfiguracao config(String idEvento) {
        return eventoConfiguracaoRepository.findByEvento_IdAndFgExcluidoFalse(idCodec.decodeEventoIdAssinado(idEvento))
                .orElseGet(() -> configPadrao(findEvento(idCodec.decodeEventoIdAssinado(idEvento))));
    }

    private EventoConfiguracao configPadrao(Evento evento) {
        EventoConfiguracao cfg = new EventoConfiguracao();
        cfg.setEvento(evento);
        cfg.setFgConsultaPublica(false);
        cfg.setFgAceitaClaim(true);
        return cfg;
    }

    private Evento findEvento(Long id) {
        return eventoRepository.findById(id)
                .filter(e -> !Boolean.TRUE.equals(e.getFgExcluido()))
                .filter(e -> Boolean.TRUE.equals(e.getFgAtivo()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento não encontrado."));
    }

    private boolean matchesPesquisa(Item item, String pesquisa) {
        String q = pesquisa.toLowerCase();
        return contains(item.getNmTitulo(), q) || contains(item.getNmMarca(), q)
                || contains(item.getNmModelo(), q) || contains(item.getNmCor(), q)
                || contains(item.getCategoria().getNmCategoria(), q);
    }

    private boolean contains(String value, String q) {
        return value != null && value.toLowerCase().contains(q);
    }

    private PortalEventoResumoResponse toEventoResumo(Evento e, Map<String, Arquivo> imgs) {
        EventoConfiguracao cfg = eventoConfiguracaoRepository.findByEvento_IdAndFgExcluidoFalse(e.getId())
                .orElseGet(() -> configPadrao(e));
        Arquivo logo = imgs != null ? imgs.get("LOGO") : null;
        Arquivo hero = imgs != null ? imgs.get("HERO") : null;
        return new PortalEventoResumoResponse(
                idCodec.encodeEventoId(e.getId()),
                e.getNmEvento(),
                e.getNmLocal(),
                e.getNmCidade(),
                e.getSgUf(),
                e.getDtInicio(),
                e.getDtFim(),
                cfg.getFgConsultaPublica(),
                cfg.getFgAceitaClaim(),
                logo != null ? idCodec.encodeArquivoId(logo.getId()) : null,
                hero != null ? idCodec.encodeArquivoId(hero.getId()) : null);
    }

    private PortalItemCatalogoResponse toCatalogoItem(Item i, Arquivo fotoPrincipal) {
        String idFoto = fotoPrincipal != null ? idCodec.encodeArquivoId(fotoPrincipal.getId()) : null;
        return new PortalItemCatalogoResponse(
                idCodec.encodeItemId(i.getId()),
                i.getCdItem(),
                i.getNmTitulo(),
                i.getCategoria().getNmCategoria(),
                i.getNmMarca(),
                i.getNmModelo(),
                i.getNmCor(),
                i.getDtEncontrado(),
                localPublico(i),
                idFoto);
    }

    /** Local exibido no card: achado → posto → local atual. */
    private static String localPublico(Item i) {
        if (i.getNmLocalEncontrado() != null && !i.getNmLocalEncontrado().isBlank()) {
            return i.getNmLocalEncontrado().trim();
        }
        if (i.getNmPosto() != null && !i.getNmPosto().isBlank()) {
            return i.getNmPosto().trim();
        }
        if (i.getLocalAtual() != null && i.getLocalAtual().getNmLocal() != null
                && !i.getLocalAtual().getNmLocal().isBlank()) {
            return i.getLocalAtual().getNmLocal().trim();
        }
        return null;
    }

    private boolean triagemConcluida(Item item) {
        return triagemRepository.findByItem_IdAndFgExcluidoFalse(item.getId())
                .filter(t -> "CONCLUIDA".equalsIgnoreCase(t.getTpStatus()))
                .isPresent();
    }

    private boolean temRetiradaPendenteNoPortal(Long itemId) {
        return claimValidacaoRepository.existsRetiradaPendenteNoPortal(itemId, CLAIM_STATUS_OCULTA_PORTAL);
    }

    private static void aplicarContatoConfianca(
            Claim claim, String nmContato, String nrTelefone, String dsRelacao) {
        claim.setNmContatoConfianca(blankToNull(nmContato));
        claim.setNrTelefoneConfianca(blankToNull(nrTelefone));
        claim.setDsRelacaoContatoConfianca(blankToNull(dsRelacao));
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    /** Detalhe público completo de um item do catálogo. */
    @Transactional(readOnly = true)
    public PortalItemDetalheResponse detalharItem(String idEvento, String idItem) {
        exigirConsultaPublica(idEvento);
        Long eventoId = idCodec.decodeEventoIdAssinado(idEvento);
        Item i = itemRepository.findById(idCodec.decodeItemIdAssinado(idItem))
                .filter(x -> !Boolean.TRUE.equals(x.getFgExcluido()))
                .filter(x -> Boolean.TRUE.equals(x.getFgAtivo()))
                .filter(x -> !Boolean.TRUE.equals(x.getFgEntregue()))
                .filter(x -> !Boolean.TRUE.equals(x.getFgDescartado()))
                .filter(x -> x.getEvento().getId().equals(eventoId))
                .filter(x -> x.getStatus() != null && STATUS_PORTAL.contains(x.getStatus().getNmStatus()))
                .filter(this::triagemConcluida)
                .filter(x -> !temRetiradaPendenteNoPortal(x.getId()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item não encontrado no catálogo público."));
        String idFoto = arquivoService.fotoPrincipalItem(i.getId())
                .map(a -> idCodec.encodeArquivoId(a.getId()))
                .orElse(null);
        List<String> idsFotos = arquivoService.fotosItem(i.getId()).stream()
                .map(a -> idCodec.encodeArquivoId(a.getId()))
                .toList();
        return new PortalItemDetalheResponse(
                idCodec.encodeItemId(i.getId()),
                i.getCdItem(),
                i.getNmTitulo(),
                i.getDsItem(),
                i.getDsObservacoes(),
                i.getCategoria() != null ? i.getCategoria().getNmCategoria() : null,
                i.getSubcategoria() != null ? i.getSubcategoria().getNmCategoria() : null,
                i.getNmMarca(),
                i.getNmModelo(),
                i.getNmCor(),
                i.getNmEstado(),
                i.getDsTags(),
                i.getDtEncontrado(),
                i.getHrEncontrado(),
                i.getNmLocalEncontrado(),
                i.getNmPosto(),
                i.getLocalAtual() != null ? i.getLocalAtual().getNmLocal() : null,
                i.getStatus() != null ? i.getStatus().getNmStatus() : null,
                i.getTpPrioridade(),
                i.getFgSensivel(),
                idFoto,
                idsFotos);
    }

    /** Streaming público da foto principal de item do catálogo. */
    @Transactional(readOnly = true)
    public ArquivoService.ArquivoConteudo baixarFotoPublica(String idArquivo) {
        return arquivoService.carregarConteudoPublicoItem(idArquivo);
    }

    /** Miniatura JPEG leve da foto pública (cards/listagens do portal). */
    @Transactional(readOnly = true)
    public ArquivoService.ArquivoConteudo baixarThumbnailPublica(String idArquivo, Integer maxEdge) {
        return arquivoService.carregarThumbnailPublicoItem(idArquivo, maxEdge);
    }

}
