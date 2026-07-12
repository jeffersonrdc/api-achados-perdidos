package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.ItemHistoricoResponse;
import br.com.achadosperdidos.controller.dto.ItemMovimentacaoCreateRequest;
import br.com.achadosperdidos.controller.dto.ItemMovimentacaoResponse;
import br.com.achadosperdidos.controller.dto.ItemTransicaoRequest;
import br.com.achadosperdidos.controller.dto.ItemTransicaoResponse;
import br.com.achadosperdidos.service.WorkflowService;
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
@Tag(name = "Workflow")
@SecurityRequirement(name = "bearerAuth")
public class WorkflowController {
    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping("/movimentacoes")
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR')")
    public ResponseEntity<ItemMovimentacaoResponse> registrarMovimentacao(
            @Valid @RequestBody ItemMovimentacaoCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workflowService.registrarMovimentacao(request));
    }

    @GetMapping("/itens/{idItem}/movimentacoes")
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR','ATENDENTE','CONSULTA')")
    public List<ItemMovimentacaoResponse> historicoItem(@PathVariable String idItem) {
        return workflowService.historicoItem(idItem);
    }

    @GetMapping("/itens/{idItem}/status-historico")
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR','ATENDENTE','CONSULTA')")
    public List<ItemHistoricoResponse> historicoStatusItem(@PathVariable String idItem) {
        return workflowService.historicoStatusItem(idItem);
    }

    @PostMapping("/itens/{idItem}/transicoes")
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR','ATENDENTE')")
    public ItemTransicaoResponse transitar(@PathVariable String idItem,
                                           @Valid @RequestBody ItemTransicaoRequest request) {
        return workflowService.transitar(idItem, request);
    }

    @GetMapping("/itens/{idItem}/transicoes-permitidas")
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR','ATENDENTE','CONSULTA')")
    public List<String> transicoesPermitidas(@PathVariable String idItem) {
        return workflowService.transicoesPermitidas(idItem);
    }
}
