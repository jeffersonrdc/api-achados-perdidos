package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.ColetaFiltrosResponse;
import br.com.achadosperdidos.controller.dto.ColetaResumoResponse;
import br.com.achadosperdidos.controller.dto.EstoqueItemResponse;
import br.com.achadosperdidos.controller.dto.EstoqueResumoResponse;
import br.com.achadosperdidos.controller.dto.ItemCreateRequest;
import br.com.achadosperdidos.controller.dto.ItemLocalizacaoRequest;
import br.com.achadosperdidos.controller.dto.ItemResponse;
import br.com.achadosperdidos.controller.dto.ItemUpdateRequest;
import br.com.achadosperdidos.entity.Evento;
import br.com.achadosperdidos.entity.Item;
import br.com.achadosperdidos.entity.Localizacao;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.pagination.PaginationMeta;
import br.com.achadosperdidos.pagination.PaginationParams;
import br.com.achadosperdidos.repository.ClaimRepository;
import br.com.achadosperdidos.repository.EventoRepository;
import br.com.achadosperdidos.repository.ItemRepository;
import br.com.achadosperdidos.repository.LocalRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ItemService {
    private final ItemRepository itemRepository;
    private final EventoRepository eventoRepository;
    private final ClaimRepository claimRepository;
    private final CategoriaService categoriaService;
    private final StatusItemService statusItemService;
    private final WorkflowService workflowService;
    private final LocalizacaoService localizacaoService;
    private final LocalRepository localRepository;
    private final SignedResourceIdCodec idCodec;
    private final UsuarioContextService usuarioContextService;
    private final AuditoriaContextService auditoriaContext;
    private final MatchService matchService;

    private static final Set<String> PRIORIDADES = Set.of("ALTA", "MEDIA", "BAIXA");
    // Status irrelevantes para o filtro de coleta (fluxo de claims).
    private static final Set<String> STATUS_OCULTOS_COLETA = Set.of(
            "Claim Aberto", "Claim em Análise", "Claim Aprovado", "Claim Rejeitado", "Claim Cancelado", "Encontrado");
    // Solicitações (claims) consideradas pendentes.
    private static final List<String> CLAIM_STATUS_PENDENTES = List.of("Claim Aberto", "Claim em Análise");

    public ItemService(ItemRepository itemRepository, EventoRepository eventoRepository,
                       ClaimRepository claimRepository, CategoriaService categoriaService,
                       StatusItemService statusItemService, WorkflowService workflowService,
                       LocalizacaoService localizacaoService, LocalRepository localRepository,
                       SignedResourceIdCodec idCodec, UsuarioContextService usuarioContextService,
                       AuditoriaContextService auditoriaContext, MatchService matchService) {
        this.itemRepository = itemRepository; this.eventoRepository = eventoRepository;
        this.claimRepository = claimRepository;
        this.categoriaService = categoriaService; this.statusItemService = statusItemService;
        this.workflowService = workflowService;
        this.localizacaoService = localizacaoService;
        this.localRepository = localRepository;
        this.idCodec = idCodec; this.usuarioContextService = usuarioContextService;
        this.auditoriaContext = auditoriaContext;
        this.matchService = matchService;
    }

    @Transactional
    public ItemResponse create(ItemCreateRequest request) {
        auditoriaContext.marcarContexto();
        Long eventoId = idCodec.decodeEventoId(request.idEvento());
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento não encontrado."));
        Item item = new Item();
        item.setEvento(evento);
        item.setCategoria(categoriaService.findEntity(idCodec.decodeCategoriaId(request.idCategoria())));
        if (request.idSubcategoria() != null && !request.idSubcategoria().isBlank()) {
            item.setSubcategoria(categoriaService.findEntity(idCodec.decodeCategoriaId(request.idSubcategoria())));
        }
        // Cadastro libera o item direto no estoque (visível em /estoque e no portal).
        item.setStatus(request.idStatus() != null && !request.idStatus().isBlank()
                ? statusItemService.findEntity(idCodec.decodeStatusId(request.idStatus()))
                : statusItemService.findByNomeOrDefault(null, "Em estoque"));
        item.setCdItem(gerarCodigoItem());
        item.setNmTitulo(request.nmTitulo().trim());
        item.setDsItem(request.dsItem());
        item.setDsObservacoes(request.dsObservacoes());
        item.setNmMarca(request.nmMarca());
        item.setNmModelo(request.nmModelo());
        item.setNmCor(request.nmCor());
        item.setNmEstado(request.nmEstado());
        item.setDsTags(blankToNull(request.dsTags()));
        item.setDtEncontrado(request.dtEncontrado());
        item.setHrEncontrado(request.hrEncontrado());
        item.setNmLocalEncontrado(request.nmLocalEncontrado());
        item.setLocalAtual(resolverLocalAtual(eventoId, request.nmLocalEncontrado()));
        item.setNmPosto(request.nmPosto());
        item.setNmEncontradoPor(request.nmEncontradoPor());
        item.setVlEstimado(request.vlEstimado());
        item.setTpPrioridade(normalizarPrioridade(request.tpPrioridade()));
        item.setFgSensivel(Boolean.TRUE.equals(request.fgSensivel()));
        // Rastreabilidade: registra quem cadastrou (usuário logado) -> exibido na coluna Operador.
        item.setUsuarioCadastro(usuarioContextService.requireUsuarioLogado());
        item.setDtCadastro(LocalDateTime.now());
        item.setFgAtivo(true);
        item.setFgExcluido(false);
        Item salvo = itemRepository.save(item);
        // Inicia a linha do tempo de status do item (secao 11 do documento).
        workflowService.registrarHistorico(salvo, null, salvo.getStatus(), null);
        matchService.recalcularMatchesPorItem(salvo);
        return toResponse(salvo);
    }

    @Transactional
    public ItemResponse update(String idToken, ItemUpdateRequest request) {
        auditoriaContext.marcarContexto();
        Item item = findEntity(idCodec.decodeItemId(idToken));
        if (request.idCategoria() != null && !request.idCategoria().isBlank()) {
            item.setCategoria(categoriaService.findEntity(idCodec.decodeCategoriaId(request.idCategoria())));
        }
        if (request.idSubcategoria() != null) {
            item.setSubcategoria(request.idSubcategoria().isBlank()
                    ? null
                    : categoriaService.findEntity(idCodec.decodeCategoriaId(request.idSubcategoria())));
        }
        if (request.idStatus() != null && !request.idStatus().isBlank()) {
            item.setStatus(statusItemService.findEntity(idCodec.decodeStatusId(request.idStatus())));
        }
        if (request.nmTitulo() != null && !request.nmTitulo().isBlank()) item.setNmTitulo(request.nmTitulo().trim());
        if (request.dsItem() != null) item.setDsItem(request.dsItem());
        if (request.dsObservacoes() != null) item.setDsObservacoes(request.dsObservacoes());
        if (request.nmMarca() != null) item.setNmMarca(request.nmMarca());
        if (request.nmModelo() != null) item.setNmModelo(request.nmModelo());
        if (request.nmCor() != null) item.setNmCor(request.nmCor());
        if (request.nmEstado() != null) item.setNmEstado(request.nmEstado());
        if (request.dsTags() != null) item.setDsTags(blankToNull(request.dsTags()));
        if (request.dtEncontrado() != null) item.setDtEncontrado(request.dtEncontrado());
        if (request.hrEncontrado() != null) item.setHrEncontrado(request.hrEncontrado());
        if (request.nmLocalEncontrado() != null) item.setNmLocalEncontrado(request.nmLocalEncontrado());
        if (request.nmPosto() != null) item.setNmPosto(request.nmPosto());
        if (request.nmEncontradoPor() != null) item.setNmEncontradoPor(request.nmEncontradoPor());
        if (request.vlEstimado() != null) item.setVlEstimado(request.vlEstimado());
        if (request.tpPrioridade() != null && !request.tpPrioridade().isBlank()) {
            item.setTpPrioridade(normalizarPrioridade(request.tpPrioridade()));
        }
        if (request.fgSensivel() != null) item.setFgSensivel(request.fgSensivel());
        item.setUsuarioAlteracao(usuarioContextService.requireUsuarioLogado());
        item.setDtAlteracao(LocalDateTime.now());
        Item salvo = itemRepository.save(item);
        matchService.recalcularMatchesPorItem(salvo);
        return toResponse(salvo);
    }

    /** Local físico inicial do item: casa o local encontrado; senão, cai no depósito do evento. */
    private br.com.achadosperdidos.entity.Local resolverLocalAtual(Long eventoId, String nmLocalEncontrado) {
        if (nmLocalEncontrado != null && !nmLocalEncontrado.isBlank()) {
            var local = localRepository.findFirstByEvento_IdAndNmLocalIgnoreCaseAndFgExcluidoFalse(eventoId, nmLocalEncontrado.trim());
            if (local.isPresent()) return local.get();
        }
        return localRepository.findFirstByEvento_IdAndTpLocalAndFgExcluidoFalse(eventoId, "DEPOSITO").orElse(null);
    }

    private String normalizarPrioridade(String prioridade) {
        if (prioridade == null || prioridade.isBlank()) return "MEDIA";
        String p = prioridade.trim().toUpperCase();
        if (!PRIORIDADES.contains(p)) {
            throw new IllegalArgumentException("Prioridade inválida: " + prioridade + ". Use ALTA, MEDIA ou BAIXA.");
        }
        return p;
    }

    @Transactional(readOnly = true)
    public ApiPage<ItemResponse> findAll(Integer page, Integer limit, String idEvento,
                                         String q, String idCategoria, String local,
                                         String tpPrioridade, String status, String data) {
        int p = PaginationParams.resolvePage(page);
        int l = PaginationParams.resolveLimit(limit);
        Long eventoId = (idEvento != null && !idEvento.isBlank()) ? idCodec.decodeEventoId(idEvento) : null;
        Long categoriaId = (idCategoria != null && !idCategoria.isBlank()) ? idCodec.decodeCategoriaId(idCategoria) : null;
        LocalDate dataEncontrado = parseData(data);

        Specification<Item> spec = filtros(eventoId, q, categoriaId, local, tpPrioridade, status, dataEncontrado);
        Page<Item> result = itemRepository.findAll(spec,
                PageRequest.of(p - 1, l, Sort.by(Sort.Direction.DESC, "dtCadastro")));
        var content = result.getContent().stream().map(this::toResponse).toList();
        var meta = new PaginationMeta(p, l, result.getTotalElements(), result.getTotalPages());
        return ApiPage.paged(content, meta);
    }

    private Specification<Item> filtros(Long eventoId, String q, Long categoriaId, String local,
                                        String tpPrioridade, String status, LocalDate data) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.isFalse(root.get("fgExcluido")));
            if (eventoId != null) ps.add(cb.equal(root.get("evento").get("id"), eventoId));
            if (categoriaId != null) ps.add(cb.equal(root.get("categoria").get("id"), categoriaId));
            if (local != null && !local.isBlank()) ps.add(cb.equal(root.get("nmLocalEncontrado"), local));
            if (tpPrioridade != null && !tpPrioridade.isBlank())
                ps.add(cb.equal(root.get("tpPrioridade"), tpPrioridade.trim().toUpperCase()));
            if (status != null && !status.isBlank()) ps.add(cb.equal(root.get("status").get("nmStatus"), status));
            if (data != null) ps.add(cb.equal(root.get("dtEncontrado"), data));
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("nmTitulo")), like),
                        cb.like(cb.lower(root.get("cdItem")), like),
                        cb.like(cb.lower(root.get("nmLocalEncontrado")), like)));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    private LocalDate parseData(String data) {
        if (data == null || data.isBlank()) return null;
        String v = data.trim();
        try {
            // aceita yyyy-MM-dd ou dd/MM/yyyy
            if (v.contains("/")) return LocalDate.parse(v, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            return LocalDate.parse(v);
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional(readOnly = true)
    public ItemResponse findById(String idToken) {
        return toResponse(findEntity(idCodec.decodeItemId(idToken)));
    }

    // ------------------------------------------------------------------
    // Coleta: cards/KPIs e filtros
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ColetaResumoResponse coletaResumo(String idEvento) {
        Long ev = idCodec.decodeEventoId(idEvento);
        return new ColetaResumoResponse(
                itemRepository.countByEvento_IdAndFgExcluidoFalse(ev),
                itemRepository.countByEvento_IdAndFgExcluidoFalseAndStatus_NmStatus(ev, "Coletado"),
                claimRepository.countByEvento_IdAndFgExcluidoFalseAndStatus_NmStatusIn(ev, CLAIM_STATUS_PENDENTES),
                itemRepository.countByEvento_IdAndFgExcluidoFalseAndStatus_NmStatus(ev, "Aguardando triagem"),
                itemRepository.countByEvento_IdAndFgExcluidoFalseAndStatus_NmStatus(ev, "Em triagem"),
                itemRepository.countByEvento_IdAndFgExcluidoFalseAndFgSensivelTrue(ev),
                itemRepository.countByEvento_IdAndFgExcluidoFalseAndTpPrioridade(ev, "ALTA"));
    }

    @Transactional(readOnly = true)
    public ColetaFiltrosResponse coletaFiltros(String idEvento) {
        Long ev = idCodec.decodeEventoId(idEvento);
        // Árvore de categorias (pai -> filhos), ids assinados.
        List<ColetaFiltrosResponse.CategoriaArvore> categorias = categoriaService.findPaisAtivos().stream()
                .map(pai -> new ColetaFiltrosResponse.CategoriaArvore(
                        idCodec.encodeCategoriaId(pai.getId()),
                        pai.getNmCategoria(),
                        categoriaService.findSubcategoriasEntidades(pai.getId()).stream()
                                .map(f -> new ColetaFiltrosResponse.Opcao(idCodec.encodeCategoriaId(f.getId()), f.getNmCategoria()))
                                .toList()))
                .toList();
        // Status do fluxo de itens (exclui claims).
        List<ColetaFiltrosResponse.Opcao> status = statusItemService.findAll().stream()
                .filter(s -> !STATUS_OCULTOS_COLETA.contains(s.nmStatus()))
                .map(s -> new ColetaFiltrosResponse.Opcao(s.id(), s.nmStatus()))
                .toList();
        List<String> locais = itemRepository.findDistinctLocais(ev);
        List<String> prioridades = List.of("ALTA", "MEDIA", "BAIXA");
        return new ColetaFiltrosResponse(categorias, status, locais, prioridades);
    }

    private static final String STATUS_ESTOQUE = "Em estoque";

    /** Estoque paginado e filtrado (server-side): itens com status "Em estoque". */
    @Transactional(readOnly = true)
    public ApiPage<EstoqueItemResponse> listarEstoque(String idEvento, Integer page, Integer limit,
                                                      String q, String idCategoria, String deposito,
                                                      String tpPrioridade, String data) {
        int p = PaginationParams.resolvePage(page);
        int l = PaginationParams.resolveLimit(limit);
        Long eventoId = idCodec.decodeEventoId(idEvento);
        Long categoriaId = (idCategoria != null && !idCategoria.isBlank()) ? idCodec.decodeCategoriaId(idCategoria) : null;
        LocalDate dataEncontrado = parseData(data);

        Specification<Item> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.isFalse(root.get("fgExcluido")));
            ps.add(cb.equal(root.get("evento").get("id"), eventoId));
            ps.add(cb.equal(root.get("status").get("nmStatus"), STATUS_ESTOQUE));
            if (categoriaId != null) ps.add(cb.equal(root.get("categoria").get("id"), categoriaId));
            if (deposito != null && !deposito.isBlank())
                ps.add(cb.equal(root.get("localizacao").get("deposito").get("nmDeposito"), deposito));
            if (tpPrioridade != null && !tpPrioridade.isBlank())
                ps.add(cb.equal(root.get("tpPrioridade"), tpPrioridade.trim().toUpperCase()));
            if (dataEncontrado != null) ps.add(cb.equal(root.get("dtEncontrado"), dataEncontrado));
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("nmTitulo")), like),
                        cb.like(cb.lower(root.get("cdItem")), like),
                        cb.like(cb.lower(root.get("nmLocalEncontrado")), like)));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<Item> result = itemRepository.findAll(spec,
                PageRequest.of(p - 1, l, Sort.by(Sort.Direction.DESC, "dtEncontrado")));
        var content = result.getContent().stream().map(this::toEstoqueResponse).toList();
        var meta = new PaginationMeta(p, l, result.getTotalElements(), result.getTotalPages());
        return ApiPage.paged(content, meta);
    }

    @Transactional(readOnly = true)
    public EstoqueResumoResponse estoqueResumo(String idEvento) {
        Long ev = idCodec.decodeEventoId(idEvento);
        List<EstoqueResumoResponse.DepositoQt> porDeposito = itemRepository.contagemEstoquePorDeposito(ev, STATUS_ESTOQUE)
                .stream()
                .map(r -> new EstoqueResumoResponse.DepositoQt(
                        r[0] != null ? r[0].toString() : "Sem localização",
                        ((Number) r[1]).longValue()))
                .toList();
        long total = porDeposito.stream().mapToLong(EstoqueResumoResponse.DepositoQt::qt).sum();
        long semLoc = porDeposito.stream()
                .filter(d -> "Sem localização".equals(d.nome())).mapToLong(EstoqueResumoResponse.DepositoQt::qt).sum();
        long comLoc = total - semLoc;
        long depositos = porDeposito.stream().filter(d -> !"Sem localização".equals(d.nome())).count();
        long sensiveis = itemRepository.countByEvento_IdAndFgExcluidoFalseAndFgSensivelTrueAndStatus_NmStatusIn(
                ev, List.of(STATUS_ESTOQUE));
        return new EstoqueResumoResponse(total, comLoc, semLoc, depositos, sensiveis, porDeposito);
    }

    /** Opções de filtro do estoque: categorias (árvore), depósitos, prioridades, status. */
    @Transactional(readOnly = true)
    public ColetaFiltrosResponse estoqueFiltros(String idEvento) {
        ColetaFiltrosResponse base = coletaFiltros(idEvento);
        Long ev = idCodec.decodeEventoId(idEvento);
        List<String> depositos = itemRepository.contagemEstoquePorDeposito(ev, STATUS_ESTOQUE).stream()
                .map(r -> r[0] != null ? r[0].toString() : "Sem localização")
                .filter(nome -> !"Sem localização".equals(nome))
                .toList();
        List<ColetaFiltrosResponse.Opcao> status = List.of(new ColetaFiltrosResponse.Opcao(STATUS_ESTOQUE, STATUS_ESTOQUE));
        return new ColetaFiltrosResponse(base.categorias(), status, depositos, base.prioridades());
    }

    /** Atualiza a localização física do item no estoque e registra a movimentação. */
    @Transactional
    public EstoqueItemResponse atualizarLocalizacao(String idItem, ItemLocalizacaoRequest req) {
        auditoriaContext.marcarContexto();
        // Estoque = endereçamento físico do item (setor/estante/caixa) dentro do depósito.
        // A movimentação entre locais é a "transferência" (outra entidade).
        Item item = findEntity(idCodec.decodeItemId(idItem));
        Long depositoId = idCodec.decode(SignedResourceIdCodec.Kind.DEP, req.idDeposito());
        Localizacao destino = localizacaoService.findOrCreateEntity(
                depositoId, req.nmSetor(), req.nmCorredor(), req.nmEstante(),
                req.nmPrateleira(), req.nmCaixa(), req.nmPosicao());
        item.setLocalizacao(destino);
        item.setUsuarioAlteracao(usuarioContextService.requireUsuarioLogado());
        item.setDtAlteracao(LocalDateTime.now());
        itemRepository.save(item);
        return toEstoqueResponse(item);
    }

    private EstoqueItemResponse toEstoqueResponse(Item i) {
        var loc = i.getLocalizacao();
        String responsavel = i.getUsuarioCadastro() != null ? i.getUsuarioCadastro().getNmUsuario() : i.getNmEncontradoPor();
        return new EstoqueItemResponse(
                idCodec.encodeItemId(i.getId()), i.getCdItem(), i.getNmTitulo(),
                i.getCategoria() != null ? i.getCategoria().getNmCategoria() : null,
                i.getSubcategoria() != null ? i.getSubcategoria().getNmCategoria() : null,
                i.getNmMarca(), i.getNmModelo(), i.getNmCor(), i.getTpPrioridade(), i.getFgSensivel(),
                i.getNmEstado(),
                i.getDtEncontrado(), i.getNmLocalEncontrado(), responsavel,
                i.getStatus() != null ? i.getStatus().getNmStatus() : null,
                loc != null && loc.getDeposito() != null ? loc.getDeposito().getNmDeposito() : null,
                loc != null ? loc.getNmSetor() : null,
                loc != null ? loc.getNmCorredor() : null,
                loc != null ? loc.getNmEstante() : null,
                loc != null ? loc.getNmPrateleira() : null,
                loc != null ? loc.getNmCaixa() : null,
                loc != null ? loc.getNmPosicao() : null);
    }

    @Transactional
    public void softDelete(String idToken) {
        auditoriaContext.marcarContexto();
        Item item = findEntity(idCodec.decodeItemId(idToken));
        item.setFgExcluido(true);
        item.setFgAtivo(false);
        item.setDtAlteracao(LocalDateTime.now());
        itemRepository.save(item);
    }

    private Item findEntity(Long id) {
        return itemRepository.findById(id)
                .filter(i -> !Boolean.TRUE.equals(i.getFgExcluido()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item não encontrado."));
    }

    private String gerarCodigoItem() {
        return "ITM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private ItemResponse toResponse(Item i) {
        String operador = i.getUsuarioCadastro() != null ? i.getUsuarioCadastro().getNmUsuario() : i.getNmEncontradoPor();
        return new ItemResponse(
                idCodec.encodeItemId(i.getId()), i.getCdItem(), i.getNmTitulo(), i.getDsItem(), i.getDsObservacoes(),
                i.getNmMarca(), i.getNmModelo(), i.getNmCor(), i.getNmEstado(), i.getDsTags(),
                i.getDtEncontrado(), i.getHrEncontrado(), i.getNmLocalEncontrado(), i.getNmPosto(),
                i.getNmEncontradoPor(), operador,
                i.getVlEstimado(), i.getStatus().getNmStatus(), i.getCategoria().getNmCategoria(),
                i.getSubcategoria() != null ? i.getSubcategoria().getNmCategoria() : null,
                i.getEvento().getNmEvento(), i.getTpPrioridade(), i.getFgSensivel(),
                i.getFgEntregue(), i.getFgDescartado(), i.getDtCadastro());
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
