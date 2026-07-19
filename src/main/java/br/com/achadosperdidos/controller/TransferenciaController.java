package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.ItemDisponivelResponse;
import br.com.achadosperdidos.controller.dto.TransferenciaCreateRequest;
import br.com.achadosperdidos.controller.dto.TransferenciaResponse;
import br.com.achadosperdidos.controller.dto.TransferenciaResumoResponse;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.service.TransferenciaService;
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
@RequestMapping("/api/v1/transferencias")
@Tag(name = "Transferências", description = "Movimentação de itens entre locais do evento.")
@SecurityRequirement(name = "bearerAuth")
public class TransferenciaController {
    private final TransferenciaService transferenciaService;

    public TransferenciaController(TransferenciaService transferenciaService) {
        this.transferenciaService = transferenciaService;
    }

    @PostMapping
    @PreAuthorize("@authz.pode('item.movimentar')")
    @Operation(summary = "Criar transferência de itens",
            description = "Registra a transferência de um ou mais itens para o local de destino.")
    public ResponseEntity<List<TransferenciaResponse>> criar(@Valid @RequestBody TransferenciaCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transferenciaService.criar(request));
    }

    @GetMapping
    @PreAuthorize("@authz.pode('item.listar')")
    @Operation(summary = "Listar transferências do evento",
            description = "Paginação baseada em `page`/`limit`, com filtros opcionais.")
    public ApiPage<TransferenciaResponse> listar(
            @Parameter(description = "ID assinado do evento", required = true) @RequestParam String idEvento,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String q,
            @Parameter(description = "ID assinado do local de destino") @RequestParam(required = false) String idLocalDestino,
            @Parameter(description = "Status da transferência") @RequestParam(required = false) String tpStatus,
            @Parameter(description = "Data no formato `yyyy-MM-dd`") @RequestParam(required = false) String data) {
        return transferenciaService.listar(idEvento, page, limit, q, idLocalDestino, tpStatus, data);
    }

    @GetMapping("/resumo")
    @PreAuthorize("@authz.pode('item.listar')")
    @Operation(summary = "Resumo de transferências do evento")
    public TransferenciaResumoResponse resumo(
            @Parameter(description = "ID assinado do evento", required = true) @RequestParam String idEvento) {
        return transferenciaService.resumo(idEvento);
    }

    @GetMapping("/itens-disponiveis")
    @PreAuthorize("@authz.pode('item.listar')")
    @Operation(summary = "Listar itens disponíveis para transferência",
            description = "Itens elegíveis à movimentação; opcionalmente filtrados pelo local de origem.")
    public List<ItemDisponivelResponse> itensDisponiveis(
            @Parameter(description = "ID assinado do evento", required = true) @RequestParam String idEvento,
            @Parameter(description = "ID assinado do local de origem") @RequestParam(required = false) String idLocalOrigem) {
        return transferenciaService.itensDisponiveis(idEvento, idLocalOrigem);
    }
}
