package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.ColetaFiltrosResponse;
import br.com.achadosperdidos.controller.dto.DevolucaoCreateRequest;
import br.com.achadosperdidos.controller.dto.DevolucaoResponse;
import br.com.achadosperdidos.controller.dto.DevolucaoStatusRequest;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.service.DevolucaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/devolucoes")
@Tag(name = "Devoluções", description = "Registro e acompanhamento de devoluções.")
@SecurityRequirement(name = "bearerAuth")
public class DevolucaoController {
    private final DevolucaoService devolucaoService;

    public DevolucaoController(DevolucaoService devolucaoService) {
        this.devolucaoService = devolucaoService;
    }

    @PostMapping
    @PreAuthorize("@authz.pode('devolucao.realizar')")
    @Operation(summary = "Registrar devolução")
    public ResponseEntity<DevolucaoResponse> create(@Valid @RequestBody DevolucaoCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(devolucaoService.create(request));
    }

    @GetMapping
    @PreAuthorize("@authz.pode('devolucao.listar')")
    @Operation(summary = "Listar devoluções (paginado)",
            description = "Filtra opcionalmente por evento (`idEvento` assinado), busca livre (`q`), local, status, prioridade e data da devolução.")
    public ApiPage<DevolucaoResponse> findAll(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit,
            @Parameter(description = "ID assinado do evento") @RequestParam(required = false) String idEvento,
            @Parameter(description = "Busca livre por recebedor, item, protocolo ou local") @RequestParam(required = false) String q,
            @RequestParam(required = false) String local,
            @Parameter(description = "Status (código ou rótulo PT)") @RequestParam(required = false) String status,
            @Parameter(description = "Prioridade do item (ALTA/MEDIA/BAIXA)") @RequestParam(required = false) String tpPrioridade,
            @Parameter(description = "Data da devolução (yyyy-MM-dd ou dd/MM/yyyy)") @RequestParam(required = false) String data) {
        return devolucaoService.findAll(page, limit, idEvento, q, local, status, tpPrioridade, data);
    }

    @GetMapping("/resumo")
    @PreAuthorize("@authz.pode('devolucao.listar')")
    @Operation(summary = "Resumo/cards das devoluções do evento")
    public br.com.achadosperdidos.controller.dto.DevolucaoResumoResponse resumo(
            @Parameter(description = "ID assinado do evento") @RequestParam String idEvento) {
        return devolucaoService.resumo(idEvento);
    }

    @GetMapping("/filtros")
    @PreAuthorize("@authz.pode('devolucao.listar')")
    @Operation(summary = "Opções de filtro das devoluções do evento",
            description = "Status fixos + locais e prioridades presentes nas devoluções do evento.")
    public ColetaFiltrosResponse filtros(
            @Parameter(description = "ID assinado do evento") @RequestParam String idEvento) {
        return devolucaoService.filtros(idEvento);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@authz.pode('devolucao.realizar')")
    @Operation(summary = "Atualizar status da devolução")
    public DevolucaoResponse atualizarStatus(@Parameter(description = "ID assinado da devolução") @PathVariable String id, @Valid @RequestBody DevolucaoStatusRequest request) {
        return devolucaoService.atualizarStatus(id, request);
    }
}
