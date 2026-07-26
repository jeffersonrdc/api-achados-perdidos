package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.ItemHistoricoResponse;
import br.com.achadosperdidos.controller.dto.ItemMovimentacaoCreateRequest;
import br.com.achadosperdidos.controller.dto.ItemMovimentacaoResponse;
import br.com.achadosperdidos.controller.dto.ItemTransicaoRequest;
import br.com.achadosperdidos.controller.dto.ItemTransicaoResponse;
import br.com.achadosperdidos.controller.dto.MovimentacaoEventoResponse;
import br.com.achadosperdidos.controller.dto.MovimentacaoResumoResponse;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.service.WorkflowService;
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
@RequestMapping("/api/v1/workflow")
@Tag(name = "Workflow", description = "Movimentações e transições de status dos itens.")
@SecurityRequirement(name = "bearerAuth")
public class WorkflowController {
    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping("/movimentacoes")
    @PreAuthorize("@authz.pode('item.movimentar')")
    @Operation(summary = "Registrar movimentação de item")
    public ResponseEntity<ItemMovimentacaoResponse> registrarMovimentacao(
            @Valid @RequestBody ItemMovimentacaoCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workflowService.registrarMovimentacao(request));
    }

    @GetMapping("/movimentacoes")
    @PreAuthorize("@authz.pode('item.listar')")
    @Operation(summary = "Listar movimentações do evento (paginado)")
    public ApiPage<MovimentacaoEventoResponse> movimentacoesPorEvento(
            @Parameter(description = "ID assinado do evento", required = true) @RequestParam String idEvento,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String tpMovimento,
            @RequestParam(required = false) String data) {
        return workflowService.listarMovimentacoesPorEvento(idEvento, page, limit, q, tpMovimento, data);
    }

    @GetMapping("/movimentacoes/resumo")
    @PreAuthorize("@authz.pode('item.listar')")
    @Operation(summary = "Resumo de movimentações do evento")
    public MovimentacaoResumoResponse resumoMovimentacoes(
            @Parameter(description = "ID assinado do evento", required = true) @RequestParam String idEvento,
            @Parameter(description = "Data (yyyy-MM-dd ou dd/MM/yyyy); sem valor = totais do evento")
            @RequestParam(required = false) String data) {
        return workflowService.resumoMovimentacoes(idEvento, data);
    }

    @GetMapping("/itens/{idItem}/movimentacoes")
    @PreAuthorize("@authz.pode('item.listar')")
    @Operation(summary = "Histórico de movimentações do item")
    public List<ItemMovimentacaoResponse> historicoItem(@Parameter(description = "ID assinado do item") @PathVariable String idItem) {
        return workflowService.historicoItem(idItem);
    }

    @GetMapping("/itens/{idItem}/status-historico")
    @PreAuthorize("@authz.pode('item.listar')")
    @Operation(summary = "Histórico de status do item")
    public List<ItemHistoricoResponse> historicoStatusItem(@Parameter(description = "ID assinado do item") @PathVariable String idItem) {
        return workflowService.historicoStatusItem(idItem);
    }

    @PostMapping("/itens/{idItem}/transicoes")
    @PreAuthorize("@authz.pode('item.transicionar')")
    @Operation(summary = "Transicionar status do item")
    public ItemTransicaoResponse transitar(@Parameter(description = "ID assinado do item") @PathVariable String idItem,
                                           @Valid @RequestBody ItemTransicaoRequest request) {
        return workflowService.transitar(idItem, request);
    }

    @GetMapping("/itens/{idItem}/transicoes-permitidas")
    @PreAuthorize("@authz.pode('item.listar')")
    @Operation(summary = "Listar transições permitidas do item")
    public List<String> transicoesPermitidas(@Parameter(description = "ID assinado do item") @PathVariable String idItem) {
        return workflowService.transicoesPermitidas(idItem);
    }
}
