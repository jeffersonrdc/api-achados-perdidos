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

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PortalService {

    /** Status que tornam o item visível na consulta pública (após triagem, no estoque). */
    private static final List<String> STATUS_PORTAL = List.of(
            "Em estoque", "Com pedido de devolucao", "Aguardando retirada");

    private final EventoRepository eventoRepository;
    private final EventoConfiguracaoRepository eventoConfiguracaoRepository;
    private final ItemRepository itemRepository;
    private final ClaimRepository claimRepository;
    private final ClaimValidacaoRepository claimValidacaoRepository;
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

    @Transactional(readOnly = true)
    public PortalEventoResumoResponse detalharEvento(String idEvento) {
        return toEventoResumo(findEvento(idCodec.decodeEventoId(idEvento)));
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
        Long eventoId = idCodec.decodeEventoId(idEvento);
        // Só aparecem no portal os itens que já passaram pela triagem e chegaram ao estoque.
        Page<Item> result = itemRepository.findByEvento_IdAndFgExcluidoFalseAndFgAtivoTrueAndFgEntregueFalseAndFgDescartadoFalseAndStatus_NmStatusIn(
                eventoId, STATUS_PORTAL, PageRequest.of(p - 1, l));
        var content = result.getContent().stream()
                .filter(i -> pesquisa == null || pesquisa.isBlank() || matchesPesquisa(i, pesquisa))
                .map(this::toCatalogoItem)
                .toList();
        return ApiPage.paged(content, new PaginationMeta(p, l, result.getTotalElements(), result.getTotalPages()));
    }

    @Transactional
    public ClaimResponse registrarObjetoPerdido(String idEvento, PortalClaimCreateRequest request) {
        exigirAceitaClaim(idEvento);
        Evento evento = findEvento(idCodec.decodeEventoId(idEvento));
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
        claim = claimRepository.save(claim);
        claim.setCdClaim(claimService.gerarProtocolo(claim.getId(), claim.getDtCadastro()));
        return claimService.toResponse(claimRepository.save(claim));
    }

    @Transactional
    public PortalClaimResultResponse reclamarItem(String idEvento, PortalClaimItemRequest request) {
        exigirAceitaClaim(idEvento);
        Long eventoId = idCodec.decodeEventoId(idEvento);
        Item item = itemRepository.findById(idCodec.decodeItemId(request.idItem()))
                .filter(i -> !Boolean.TRUE.equals(i.getFgExcluido()))
                .filter(i -> i.getEvento().getId().equals(eventoId))
                .filter(i -> !Boolean.TRUE.equals(i.getFgEntregue()))
                .filter(i -> !Boolean.TRUE.equals(i.getFgDescartado()))
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
        claim.setDsObjeto(request.dsObservacao());
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

    /** Upload público de foto para relato de perda (CLAIM / FOTO). */
    @Transactional
    public ArquivoResponse uploadFotoClaim(String idEvento, String idClaim, org.springframework.web.multipart.MultipartFile file) {
        exigirAceitaClaim(idEvento);
        Long eventoId = idCodec.decodeEventoId(idEvento);
        Claim claim = claimRepository.findById(idCodec.decodeClaimId(idClaim))
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

    @Transactional(readOnly = true)
    public List<ClaimResponse> meusClaims(String idEvento, String email) {
        exigirAceitaClaim(idEvento);
        return claimRepository.findByNmEmailIgnoreCaseAndEvento_IdAndFgExcluidoFalseOrderByDtCadastroDesc(
                        email.trim().toLowerCase(), idCodec.decodeEventoId(idEvento))
                .stream().map(claimService::toResponse).toList();
    }

    @Transactional
    public CriancaResponse cadastrarCrianca(String idEvento, CriancaCreateRequest request) {
        if (!idEvento.equals(request.idEvento())) {
            throw new IllegalArgumentException("O evento do corpo deve coincidir com o evento do portal.");
        }
        findEvento(idCodec.decodeEventoId(idEvento));
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
        EventoConfiguracao cfg = config(idEvento);
        if (!Boolean.TRUE.equals(cfg.getFgConsultaPublica())) {
            throw new PortalIndisponivelException("Consulta pública desabilitada para este evento.");
        }
    }

    private void exigirAceitaClaim(String idEvento) {
        EventoConfiguracao cfg = config(idEvento);
        if (!Boolean.TRUE.equals(cfg.getFgAceitaClaim())) {
            throw new PortalIndisponivelException("Registro de objetos perdidos desabilitado para este evento.");
        }
    }

    private EventoConfiguracao config(String idEvento) {
        return eventoConfiguracaoRepository.findByEvento_IdAndFgExcluidoFalse(idCodec.decodeEventoId(idEvento))
                .orElseGet(() -> configPadrao(findEvento(idCodec.decodeEventoId(idEvento))));
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

    private PortalItemCatalogoResponse toCatalogoItem(Item i) {
        return new PortalItemCatalogoResponse(
                idCodec.encodeItemId(i.getId()),
                i.getNmTitulo(),
                i.getCategoria().getNmCategoria(),
                i.getNmMarca(),
                i.getNmModelo(),
                i.getNmCor(),
                i.getDtEncontrado(),
                i.getNmLocalEncontrado());
    }

}
