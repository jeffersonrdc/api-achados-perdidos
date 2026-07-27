package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.ColetaFiltrosResponse;
import br.com.achadosperdidos.controller.dto.DevolucaoCreateRequest;
import br.com.achadosperdidos.controller.dto.DevolucaoResponse;
import br.com.achadosperdidos.controller.dto.DevolucaoStatusRequest;
import br.com.achadosperdidos.entity.Claim;
import br.com.achadosperdidos.entity.Devolucao;
import br.com.achadosperdidos.entity.Item;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.pagination.PaginationMeta;
import br.com.achadosperdidos.pagination.PaginationParams;
import br.com.achadosperdidos.repository.ClaimRepository;
import br.com.achadosperdidos.repository.DevolucaoRepository;
import br.com.achadosperdidos.repository.ItemRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DevolucaoService {
    private static final Map<String, String> STATUS_LABEL = Map.of(
            "AGUARDANDO_RETIRADA", "Aguardando retirada",
            "EM_CONFERENCIA", "Em conferência",
            "AGUARDANDO_ASSINATURA", "Aguardando assinatura",
            "ASSINADO", "Assinado",
            "CONCLUIDO", "Concluído");

    private static final Map<String, String> STATUS_CODE = Map.of(
            "aguardando retirada", "AGUARDANDO_RETIRADA",
            "em conferência", "EM_CONFERENCIA",
            "em conferencia", "EM_CONFERENCIA",
            "aguardando assinatura", "AGUARDANDO_ASSINATURA",
            "assinado", "ASSINADO",
            "concluído", "CONCLUIDO",
            "concluido", "CONCLUIDO");

    private final DevolucaoRepository devolucaoRepository;
    private final ItemRepository itemRepository;
    private final ClaimRepository claimRepository;
    private final WorkflowService workflowService;
    private final MatchService matchService;
    private final AuditoriaContextService auditoriaContext;
    private final SignedResourceIdCodec idCodec;
    private final DevolucaoFluxoService devolucaoFluxoService;

    public DevolucaoService(DevolucaoRepository devolucaoRepository, ItemRepository itemRepository, ClaimRepository claimRepository,
                            WorkflowService workflowService, MatchService matchService,
                            AuditoriaContextService auditoriaContext, SignedResourceIdCodec idCodec,
                            @org.springframework.context.annotation.Lazy DevolucaoFluxoService devolucaoFluxoService) {
        this.devolucaoRepository = devolucaoRepository;
        this.itemRepository = itemRepository;
        this.claimRepository = claimRepository;
        this.workflowService = workflowService;
        this.matchService = matchService;
        this.auditoriaContext = auditoriaContext;
        this.idCodec = idCodec;
        this.devolucaoFluxoService = devolucaoFluxoService;
    }

    @Transactional
    public DevolucaoResponse create(DevolucaoCreateRequest request) {
        auditoriaContext.marcarContexto();
        Item item = itemRepository.findById(idCodec.decodeItemId(request.idItem()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item não encontrado."));
        Devolucao d = new Devolucao();
        d.setEvento(item.getEvento());
        d.setItem(item);
        Long claimId = null;
        if (request.idClaim() != null && !request.idClaim().isBlank()) {
            Claim claim = claimRepository.findById(idCodec.decodeClaimId(request.idClaim()))
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Claim não encontrado."));
            if (!claim.getEvento().getId().equals(item.getEvento().getId())) {
                throw new IllegalArgumentException("Claim e item pertencem a eventos diferentes — devolução não permitida.");
            }
            d.setClaim(claim);
            claimId = claim.getId();
        }
        d.setTpDevolucao(request.tpDevolucao());
        d.setNmRecebedor(request.nmRecebedor());
        d.setNrCpf(request.nrCpf());
        d.setDsObservacao(request.dsObservacao());
        d.setFgAssinado(Boolean.TRUE.equals(request.fgAssinado()));
        d.setFgConcluido(Boolean.TRUE.equals(request.fgConcluido()));
        d.setTpStatus(Boolean.TRUE.equals(request.fgConcluido()) ? "CONCLUIDO"
                : Boolean.TRUE.equals(request.fgAssinado()) ? "ASSINADO" : "AGUARDANDO_RETIRADA");
        d.setDtDevolucao(LocalDateTime.now());
        d.setDtCadastro(LocalDateTime.now());
        d.setFgAtivo(true);
        d.setFgExcluido(false);
        if (Boolean.TRUE.equals(d.getFgConcluido())) {
            item.setFgEntregue(true);
            item.setDtAlteracao(LocalDateTime.now());
            itemRepository.save(item);
            // Fecha o ciclo: se o status atual permitir, marca o item como Devolvido.
            workflowService.transitarSePermitido(request.idItem(), "Devolvido", "Devolução concluída ao responsável.");
        }
        Devolucao saved = devolucaoRepository.save(d);
        if (saved.getCdProtocolo() == null || saved.getCdProtocolo().isBlank()) {
            saved.setCdProtocolo(String.format(java.util.Locale.ROOT, "DEV-%d-%06d",
                    LocalDateTime.now().getYear(), saved.getId()));
            saved = devolucaoRepository.save(saved);
        }
        if (claimId != null) {
            matchService.confirmarMatch(claimId, item.getId());
        }
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ApiPage<DevolucaoResponse> findAll(Integer page, Integer limit, String idEvento,
                                              String q, String local, String status,
                                              String tpPrioridade, String data) {
        int p = PaginationParams.resolvePage(page);
        int l = PaginationParams.resolveLimit(limit);
        Long eventoId = (idEvento != null && !idEvento.isBlank()) ? idCodec.decodeEventoId(idEvento) : null;
        String statusCode = normalizarStatus(status);
        LocalDate dataDevolucao = parseData(data);

        Specification<Devolucao> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.isFalse(root.get("fgExcluido")));
            if (eventoId != null) ps.add(cb.equal(root.get("evento").get("id"), eventoId));
            if (statusCode != null) ps.add(cb.equal(root.get("tpStatus"), statusCode));
            if (dataDevolucao != null) {
                ps.add(cb.between(root.get("dtDevolucao"),
                        dataDevolucao.atStartOfDay(), dataDevolucao.atTime(LocalTime.MAX)));
            }
            Join<Devolucao, Item> item = root.join("item");
            if (local != null && !local.isBlank()) {
                ps.add(cb.equal(item.get("nmLocalEncontrado"), local));
            }
            if (tpPrioridade != null && !tpPrioridade.isBlank()) {
                ps.add(cb.equal(item.get("tpPrioridade"), tpPrioridade.trim().toUpperCase()));
            }
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("nmRecebedor")), like),
                        cb.like(cb.lower(item.get("nmTitulo")), like),
                        cb.like(cb.lower(item.get("cdItem")), like),
                        cb.like(cb.lower(item.get("nmLocalEncontrado")), like)));
            }
            // Evita duplicatas quando o join com item é usado em contagem.
            if (query != null && Long.class != query.getResultType() && long.class != query.getResultType()) {
                query.distinct(true);
                var statusPath = root.get("tpStatus");
                Expression<Integer> grupoConcluido = cb.<Integer>selectCase()
                        .when(statusPath.in("COMPLETED", "CONCLUIDO", "DELIVERED", "CANCELLED", "ITEM_DELIVERED"), 1)
                        .otherwise(0);
                Expression<Integer> ordemPipeline = cb.<Integer>selectCase()
                        .when(cb.equal(statusPath, "DELIVERY_METHOD_PENDING"), 1)
                        .when(cb.equal(statusPath, "CREATED"), 2)
                        .when(cb.equal(statusPath, "PICKUP_SELECTED"), 10)
                        .when(cb.equal(statusPath, "PICKUP_SCHEDULE_REQUESTED"), 11)
                        .when(cb.equal(statusPath, "PICKUP_OPTIONS_PREPARED"), 12)
                        .when(cb.equal(statusPath, "PICKUP_OPTIONS_SENT"), 13)
                        .when(cb.equal(statusPath, "PICKUP_CONFIRMATION_PENDING"), 14)
                        .when(cb.equal(statusPath, "PICKUP_OPTIONS_EXPIRED"), 15)
                        .when(cb.equal(statusPath, "PICKUP_SCHEDULE_CONFIRMED"), 16)
                        .when(cb.equal(statusPath, "READY_FOR_PICKUP"), 17)
                        .when(cb.equal(statusPath, "AGUARDANDO_RETIRADA"), 18)
                        .when(cb.equal(statusPath, "EM_CONFERENCIA"), 19)
                        .when(cb.equal(statusPath, "TERMO_GERADO"), 20)
                        .when(cb.equal(statusPath, "AGUARDANDO_ASSINATURA"), 21)
                        .when(cb.equal(statusPath, "ASSINADO"), 22)
                        .when(cb.equal(statusPath, "SHIPPING_SELECTED"), 30)
                        .when(cb.equal(statusPath, "SHIPPING_ADDRESS_PENDING"), 31)
                        .when(cb.equal(statusPath, "SHIPPING_QUOTE_PENDING"), 32)
                        .when(cb.equal(statusPath, "SHIPPING_QUOTE_SENT"), 33)
                        .when(cb.equal(statusPath, "PAYMENT_PROOF_PENDING"), 34)
                        .when(cb.equal(statusPath, "PAID_AWAITING_POSTING"), 35)
                        .when(cb.equal(statusPath, "POSTED"), 36)
                        .when(cb.equal(statusPath, "IN_TRANSIT"), 37)
                        .otherwise(50);
                query.orderBy(
                        cb.asc(grupoConcluido),
                        cb.asc(ordemPipeline),
                        cb.desc(root.get("dtDevolucao")));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<Devolucao> result = devolucaoRepository.findAll(spec, PageRequest.of(p - 1, l));
        var content = result.getContent().stream().map(this::toResponse).toList();
        return ApiPage.paged(content, new PaginationMeta(p, l, result.getTotalElements(), result.getTotalPages()));
    }

    /** Opções de filtro da tela de devoluções (status fixos + locais/prioridades presentes). */
    @Transactional(readOnly = true)
    public ColetaFiltrosResponse filtros(String idEvento) {
        Long ev = idCodec.decodeEventoId(idEvento);
        var status = List.of(
                new ColetaFiltrosResponse.Opcao("AGUARDANDO_RETIRADA", "Aguardando retirada"),
                new ColetaFiltrosResponse.Opcao("EM_CONFERENCIA", "Em conferência"),
                new ColetaFiltrosResponse.Opcao("AGUARDANDO_ASSINATURA", "Aguardando assinatura"),
                new ColetaFiltrosResponse.Opcao("ASSINADO", "Assinado"),
                new ColetaFiltrosResponse.Opcao("CONCLUIDO", "Concluído"));
        var locais = devolucaoRepository.findDistinctLocais(ev);
        var prioridades = devolucaoRepository.findDistinctPrioridades(ev);
        if (prioridades.isEmpty()) {
            prioridades = List.of("ALTA", "MEDIA", "BAIXA");
        }
        return new ColetaFiltrosResponse(List.of(), status, locais, prioridades);
    }

    /** Aceita código (AGUARDANDO_RETIRADA) ou rótulo PT ("Aguardando retirada"). */
    private String normalizarStatus(String status) {
        if (status == null || status.isBlank()) return null;
        String v = status.trim();
        if (STATUS_LABEL.containsKey(v.toUpperCase())) return v.toUpperCase();
        return STATUS_CODE.get(v.toLowerCase());
    }

    /** Aceita yyyy-MM-dd ou dd/MM/yyyy; retorna null se vazio/inválido. */
    private LocalDate parseData(String data) {
        if (data == null || data.isBlank()) return null;
        String v = data.trim();
        try {
            if (v.contains("/")) return LocalDate.parse(v, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            return LocalDate.parse(v);
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional(readOnly = true)
    public br.com.achadosperdidos.controller.dto.DevolucaoResumoResponse resumo(String idEvento, String data) {
        Long ev = idCodec.decodeEventoId(idEvento);
        LocalDate dia = parseData(data);

        Specification<Devolucao> base = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.isFalse(root.get("fgExcluido")));
            ps.add(cb.equal(root.get("evento").get("id"), ev));
            if (dia != null) {
                ps.add(cb.between(root.get("dtDevolucao"),
                        dia.atStartOfDay(), dia.atTime(LocalTime.MAX)));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };

        return new br.com.achadosperdidos.controller.dto.DevolucaoResumoResponse(
                devolucaoRepository.count(base),
                devolucaoRepository.count(base.and((root, query, cb) ->
                        cb.equal(root.get("tpStatus"), "AGUARDANDO_RETIRADA"))),
                devolucaoRepository.count(base.and((root, query, cb) ->
                        cb.equal(root.get("tpStatus"), "EM_CONFERENCIA"))),
                devolucaoRepository.count(base.and((root, query, cb) ->
                        cb.equal(root.get("tpStatus"), "AGUARDANDO_ASSINATURA"))),
                devolucaoRepository.count(base.and((root, query, cb) ->
                        cb.equal(root.get("tpStatus"), "ASSINADO"))),
                devolucaoRepository.count(base.and((root, query, cb) ->
                        cb.equal(root.get("tpStatus"), "CONCLUIDO"))));
    }

    /** Avança o status da devolução; ao concluir, marca o item como Devolvido. */
    @Transactional
    public DevolucaoResponse atualizarStatus(String idToken, DevolucaoStatusRequest request) {
        return devolucaoFluxoService.atualizarStatus(idToken, request);
    }

    private DevolucaoResponse toResponse(Devolucao d) {
        Item item = d.getItem();
        Claim claim = d.getClaim();
        return new DevolucaoResponse(
                idCodec.encodeDevolucaoId(d.getId()),
                idCodec.encodeItemId(item.getId()),
                claim != null ? idCodec.encodeClaimId(claim.getId()) : null,
                item.getCdItem(),
                item.getNmTitulo(),
                item.getCategoria() != null ? item.getCategoria().getNmCategoria() : null,
                item.getNmLocalEncontrado(),
                d.getTpDevolucao(), d.getNmRecebedor(), d.getTpStatus(), d.getFgAssinado(), d.getFgConcluido(), d.getDtDevolucao(),
                d.getNrCpf() != null ? d.getNrCpf() : (claim != null ? claim.getNrCpf() : null),
                claim != null ? claim.getNmEmail() : null,
                claim != null ? claim.getNrTelefone() : null,
                item.getDtEncontrado(),
                d.getDsObservacao(),
                item.getTpPrioridade(),
                item.getFgSensivel(),
                d.getCdProtocolo(),
                d.getTpMetodo(),
                d.getTpClaimOrigem() != null ? d.getTpClaimOrigem() : (claim != null ? claim.getTpClaim() : null),
                DevolucaoStatusMachine.nextAction(d.getTpStatus()),
                Boolean.TRUE.equals(d.getFgAtualizacaoOperador()));
    }
}
