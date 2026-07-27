package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.ClaimAprovarRequest;
import br.com.achadosperdidos.controller.dto.ClaimCreateRequest;
import br.com.achadosperdidos.controller.dto.ClaimHistoricoResponse;
import br.com.achadosperdidos.controller.dto.ClaimMensagemCreateRequest;
import br.com.achadosperdidos.controller.dto.ClaimMensagemResponse;
import br.com.achadosperdidos.controller.dto.ClaimReprovarRequest;
import br.com.achadosperdidos.controller.dto.ClaimResponse;
import br.com.achadosperdidos.controller.dto.ClaimSolicitarInfoRequest;
import br.com.achadosperdidos.controller.dto.ClaimUpdateRequest;
import br.com.achadosperdidos.controller.dto.DevolucaoResponse;
import br.com.achadosperdidos.controller.dto.MatchCandidatoResponse;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.service.ClaimMensagemService;
import br.com.achadosperdidos.service.ClaimService;
import br.com.achadosperdidos.service.ClaimWorkflowService;
import br.com.achadosperdidos.service.DevolucaoFluxoService;
import br.com.achadosperdidos.service.MatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/claims")
@Tag(name = "Claims", description = "Relatos de perda (PERDA) e pedidos de retirada (RETIRADA).")
@SecurityRequirement(name = "bearerAuth")
public class ClaimController {
    private final ClaimService claimService;
    private final ClaimWorkflowService claimWorkflowService;
    private final ClaimMensagemService claimMensagemService;
    private final MatchService matchService;
    private final DevolucaoFluxoService devolucaoFluxoService;

    public ClaimController(ClaimService claimService, ClaimWorkflowService claimWorkflowService,
                           ClaimMensagemService claimMensagemService, MatchService matchService,
                           DevolucaoFluxoService devolucaoFluxoService) {
        this.claimService = claimService;
        this.claimWorkflowService = claimWorkflowService;
        this.claimMensagemService = claimMensagemService;
        this.matchService = matchService;
        this.devolucaoFluxoService = devolucaoFluxoService;
    }

    @PostMapping
    @PreAuthorize("@authz.pode('claim.criar')")
    @Operation(summary = "Criar claim (backoffice)")
    public ResponseEntity<ClaimResponse> create(@Valid @RequestBody ClaimCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(claimService.create(request));
    }

    @GetMapping
    @PreAuthorize("@authz.pode('claim.listar')")
    @Operation(summary = "Listar claims (paginado)",
            description = "Filtra por evento, tipo (`PERDA`/`RETIRADA`), busca livre, categoria, local, status e data.")
    public ApiPage<ClaimResponse> findAll(@RequestParam(required = false) Integer page,
                                          @RequestParam(required = false) Integer limit,
                                          @Parameter(description = "ID assinado do evento") @RequestParam(required = false) String idEvento,
                                          @Parameter(description = "Busca livre por nome, objeto, local ou protocolo") @RequestParam(required = false) String q,
                                          @Parameter(description = "ID assinado da categoria") @RequestParam(required = false) String idCategoria,
                                          @RequestParam(required = false) String local,
                                          @RequestParam(required = false) String status,
                                          @Parameter(description = "Data de cadastro (yyyy-MM-dd ou dd/MM/yyyy)") @RequestParam(required = false) String data,
                                          @Parameter(description = "PERDA ou RETIRADA") @RequestParam(required = false) String tipo) {
        return claimService.findAll(page, limit, idEvento, q, idCategoria, local, status, data, tipo);
    }

    @GetMapping("/resumo")
    @PreAuthorize("@authz.pode('claim.listar')")
    @Operation(summary = "Resumo/cards dos claims do evento")
    public br.com.achadosperdidos.controller.dto.ClaimResumoResponse resumo(
            @Parameter(description = "ID assinado do evento") @RequestParam String idEvento,
            @Parameter(description = "PERDA ou RETIRADA") @RequestParam(required = false) String tipo,
            @Parameter(description = "Data de cadastro (yyyy-MM-dd ou dd/MM/yyyy); sem valor = totais do evento")
            @RequestParam(required = false) String data) {
        return claimService.resumo(idEvento, tipo, data);
    }

    @GetMapping("/filtros")
    @PreAuthorize("@authz.pode('claim.listar')")
    @Operation(summary = "Opções de filtro dos claims do evento")
    public br.com.achadosperdidos.controller.dto.ColetaFiltrosResponse filtros(
            @Parameter(description = "ID assinado do evento") @RequestParam String idEvento,
            @Parameter(description = "PERDA ou RETIRADA") @RequestParam(required = false) String tipo) {
        return claimService.filtros(idEvento, tipo);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.pode('claim.listar')")
    @Operation(summary = "Buscar claim por ID assinado")
    public ClaimResponse findById(@PathVariable String id) { return claimService.findById(id); }

    @GetMapping("/{id}/matches")
    @PreAuthorize("@authz.pode('claim.listar')")
    @Operation(summary = "Candidatos da coleta com match pendente para o claim PERDA",
            description = "Retorna itens sugeridos (score ≥ 55) persistidos em claim_validacao com status PENDENTE.")
    public List<MatchCandidatoResponse> matches(@PathVariable String id) {
        return matchService.listarPorClaim(id);
    }

    @PostMapping("/{id}/matches/recalcular")
    @PreAuthorize("@authz.podeQualquer('claim.editar', 'claim.criar', 'claim.listar')")
    @Operation(summary = "Recalcular matches do claim PERDA",
            description = "Pesquisa novamente na coleta (novos itens podem ter entrado) e atualiza status Match / Aguardando Match.")
    public ClaimResponse recalcularMatches(@PathVariable String id) {
        return claimService.recalcularMatches(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.podeQualquer('claim.editar', 'claim.criar')")
    @Operation(summary = "Atualizar claim")
    public ClaimResponse update(@PathVariable String id, @Valid @RequestBody ClaimUpdateRequest request) {
        return claimService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.pode('claim.excluir')")
    @Operation(summary = "Excluir claim (soft delete)")
    public ResponseEntity<Void> delete(@PathVariable String id) { claimService.softDelete(id); return ResponseEntity.noContent().build(); }

    @PostMapping("/{id}/analise")
    @PreAuthorize("@authz.pode('claim.validar')")
    @Operation(summary = "Iniciar análise do claim",
            description = "Transição de workflow. Dispara e-mail de análise quando configurado (A04).")
    public ClaimResponse iniciarAnalise(@PathVariable String id) {
        return claimWorkflowService.iniciarAnalise(id);
    }

    @PostMapping("/{id}/solicitar-info")
    @PreAuthorize("@authz.pode('claim.validar')")
    @Operation(summary = "Solicitar informações adicionais ao reclamante")
    public ClaimResponse solicitarInfo(@PathVariable String id, @Valid @RequestBody ClaimSolicitarInfoRequest request) {
        return claimWorkflowService.solicitarInfo(id, request);
    }

    @PostMapping("/{id}/aprovar")
    @PreAuthorize("@authz.pode('claim.validar')")
    @Operation(summary = "Aprovar claim",
            description = "Aprovação do pedido de devolução. Pode vincular item e registrar histórico.")
    public ClaimResponse aprovar(@PathVariable String id, @Valid @RequestBody ClaimAprovarRequest request) {
        return claimWorkflowService.aprovar(id, request);
    }

    @PostMapping("/{id}/reprovar")
    @PreAuthorize("@authz.pode('claim.validar')")
    @Operation(summary = "Reprovar claim")
    public ClaimResponse reprovar(@PathVariable String id, @Valid @RequestBody ClaimReprovarRequest request) {
        return claimWorkflowService.reprovar(id, request);
    }

    @GetMapping("/{id}/historico")
    @PreAuthorize("@authz.pode('claim.listar')")
    @Operation(summary = "Histórico do claim", description = "Trilha de mudanças de status (A09).")
    public List<ClaimHistoricoResponse> historico(@PathVariable String id) {
        return claimWorkflowService.historico(id);
    }

    @GetMapping("/{id}/mensagens")
    @PreAuthorize("@authz.pode('claim.listar')")
    @Operation(summary = "Listar mensagens da conversa do claim",
            description = "Também marca as mensagens do solicitante como vistas pelo operador.")
    public List<ClaimMensagemResponse> mensagens(@PathVariable String id) {
        return claimMensagemService.listar(id);
    }

    @PostMapping("/{id}/mensagens")
    @PreAuthorize("@authz.pode('claim.validar')")
    @Operation(summary = "Enviar mensagem na conversa (dispara e-mail e renova o link)")
    public ClaimMensagemResponse enviarMensagem(@PathVariable String id,
                                                @Valid @RequestBody ClaimMensagemCreateRequest request) {
        return claimMensagemService.enviarOperador(id, request);
    }

    @PostMapping("/{id}/returns")
    @PreAuthorize("@authz.pode('claim.validar')")
    @Operation(summary = "Criar ticket de devolução a partir do claim aprovado (idempotente)")
    public DevolucaoResponse criarRetorno(@PathVariable String id) {
        return devolucaoFluxoService.criarRetornoDoClaim(id);
    }
}
