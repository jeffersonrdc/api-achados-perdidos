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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class PortalService {

    /** Status que tornam o item visível na consulta pública (após triagem, no estoque). */
    private static final List<String> STATUS_PORTAL = List.of(
            "Em estoque", "Com pedido de devolucao", "Aguardando retirada");
    private static final int MAX_COMPROVANTES_RETIRADA = 5;
    private static final long MAX_BYTES_COMPROVANTE = 10L * 1024 * 1024;
    private static final Set<String> MIME_COMPROVANTE = Set.of(
            "image/jpeg", "image/pjpeg", "image/png", "application/pdf");

    private final EventoRepository eventoRepository;
    private final EventoConfiguracaoRepository eventoConfiguracaoRepository;
    private final ItemRepository itemRepository;
    private final ClaimRepository claimRepository;
    private final ClaimValidacaoRepository claimValidacaoRepository;
    private final ArquivoRepository arquivoRepository;
    private final ClaimService claimService;
    private final ArquivoService arquivoService;
    private final CategoriaService categoriaService;
    private final TagService tagService;
    private final LocalService localService;
    private final StatusItemService statusItemService;
    private final CriancaService criancaService;
    private final PerfilRepository perfilRepository;
    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final SignedResourceIdCodec idCodec;

    public PortalService(EventoRepository eventoRepository,
                         EventoConfiguracaoRepository eventoConfiguracaoRepository,
                         ItemRepository itemRepository,
                         ClaimRepository claimRepository,
                         ClaimValidacaoRepository claimValidacaoRepository,
                         ArquivoRepository arquivoRepository,
                         ClaimService claimService,
                         ArquivoService arquivoService,
                         CategoriaService categoriaService,
                         TagService tagService,
                         LocalService localService,
                         StatusItemService statusItemService,
                         CriancaService criancaService,
                         PerfilRepository perfilRepository,
                         EmpresaRepository empresaRepository,
                         UsuarioRepository usuarioRepository,
                         PasswordEncoder passwordEncoder,
                         SignedResourceIdCodec idCodec) {
        this.eventoRepository = eventoRepository;
        this.eventoConfiguracaoRepository = eventoConfiguracaoRepository;
        this.itemRepository = itemRepository;
        this.claimRepository = claimRepository;
        this.claimValidacaoRepository = claimValidacaoRepository;
        this.arquivoRepository = arquivoRepository;
        this.claimService = claimService;
        this.arquivoService = arquivoService;
        this.categoriaService = categoriaService;
        this.tagService = tagService;
        this.localService = localService;
        this.statusItemService = statusItemService;
        this.criancaService = criancaService;
        this.perfilRepository = perfilRepository;
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.idCodec = idCodec;
    }

    @Transactional(readOnly = true)
    public List<PortalEventoResumoResponse> listarEventosAbertos() {
        return eventoRepository.findByFgExcluidoFalseAndFgAtivoTrueOrderByDtInicioDesc().stream()
                .map(this::toEventoResumo)
                .filter(e -> Boolean.TRUE.equals(e.fgConsultaPublica()) || Boolean.TRUE.equals(e.fgAceitaClaim()))
                .toList();
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
        return toEventoResumo(e);
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
        // Só aparecem no portal os itens que já passaram pela triagem e chegaram ao estoque.
        Page<Item> result = itemRepository.findByEvento_IdAndFgExcluidoFalseAndFgAtivoTrueAndFgEntregueFalseAndFgDescartadoFalseAndStatus_NmStatusIn(
                eventoId, STATUS_PORTAL, PageRequest.of(p - 1, l));
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
        claim.setDtCadastro(LocalDateTime.now());
        claim.setFgAtivo(true);
        claim.setFgExcluido(false);
        aplicarContatoConfianca(claim,
                request.nmContatoConfianca(), request.nrTelefoneConfianca(), request.dsRelacaoContatoConfianca());
        claim = claimRepository.save(claim);
        claim.setCdClaim(claimService.gerarProtocolo(claim.getId(), claim.getDtCadastro()));
        return claimService.toResponse(claimRepository.save(claim));
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
        Empresa empresa = empresaRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Nenhuma empresa cadastrada."));

        Usuario u = new Usuario();
        u.setEmpresa(empresa);
        u.setPerfil(perfil);
        u.setNmUsuario(request.nmUsuario().trim());
        u.setNmLogin(request.nmEmail().trim().toLowerCase());
        u.setNmEmail(request.nmEmail().trim().toLowerCase());
        u.setNmSenha(passwordEncoder.encode(request.senha()));
        u.setDtCadastro(LocalDateTime.now());
        u.setFgAtivo(true);
        u.setFgExcluido(false);
        u = usuarioRepository.save(u);
        return new UsuarioResponse(
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

    private PortalEventoResumoResponse toEventoResumo(Evento e) {
        EventoConfiguracao cfg = eventoConfiguracaoRepository.findByEvento_IdAndFgExcluidoFalse(e.getId())
                .orElseGet(() -> configPadrao(e));
        return new PortalEventoResumoResponse(
                idCodec.encodeEventoId(e.getId()),
                e.getNmEvento(),
                e.getNmLocal(),
                e.getNmCidade(),
                e.getSgUf(),
                e.getDtInicio(),
                e.getDtFim(),
                cfg.getFgConsultaPublica(),
                cfg.getFgAceitaClaim());
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
                i.getNmLocalEncontrado(),
                idFoto);
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
