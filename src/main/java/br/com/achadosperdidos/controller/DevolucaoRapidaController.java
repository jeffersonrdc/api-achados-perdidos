package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.ClaimCreateItemRequest;
import br.com.achadosperdidos.controller.dto.ColetaFiltrosResponse;
import br.com.achadosperdidos.controller.dto.DevolucaoRapidaResponse;
import br.com.achadosperdidos.controller.dto.EstoqueItemResponse;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.service.ClaimService;
import br.com.achadosperdidos.service.ItemService;
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
@RequestMapping("/api/v1/devolucao-rapida")
@Tag(name = "Devolução Rápida", description = "Retirada presencial no evento: lista itens disponíveis e conclui a entrega.")
@SecurityRequirement(name = "bearerAuth")
public class DevolucaoRapidaController {

    private static final String PERMISSAO =
            "@authz.pode('devolucao-rapida.acessar') or @authz.pode('claim.criar')";

    private final ItemService itemService;
    private final ClaimService claimService;

    public DevolucaoRapidaController(ItemService itemService, ClaimService claimService) {
        this.itemService = itemService;
        this.claimService = claimService;
    }

    @GetMapping
    @PreAuthorize(PERMISSAO)
    @Operation(summary = "Listar itens disponíveis para devolução rápida",
            description = "Somente itens Em estoque, não entregues e sem pedido de retirada em aberto.")
    public ApiPage<EstoqueItemResponse> listar(
            @Parameter(description = "ID assinado do evento", required = true) @RequestParam String idEvento,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String idCategoria,
            @RequestParam(required = false) String deposito,
            @RequestParam(required = false) String tpPrioridade,
            @RequestParam(required = false) String data) {
        return itemService.listarEstoqueDevolucaoRapida(
                idEvento, page, limit, q, idCategoria, deposito, tpPrioridade, data);
    }

    @GetMapping("/itens")
    @PreAuthorize(PERMISSAO)
    @Operation(summary = "Listar itens disponíveis para devolução rápida")
    public ApiPage<EstoqueItemResponse> listarItens(
            @Parameter(description = "ID assinado do evento", required = true) @RequestParam String idEvento,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String idCategoria,
            @RequestParam(required = false) String deposito,
            @RequestParam(required = false) String tpPrioridade,
            @RequestParam(required = false) String data) {
        return listar(idEvento, page, limit, q, idCategoria, deposito, tpPrioridade, data);
    }

    @GetMapping("/filtros")
    @PreAuthorize(PERMISSAO)
    @Operation(summary = "Filtros da tela de devolução rápida")
    public ColetaFiltrosResponse filtros(
            @Parameter(description = "ID assinado do evento", required = true) @RequestParam String idEvento) {
        return itemService.estoqueFiltros(idEvento);
    }

    @PostMapping
    @PreAuthorize(PERMISSAO)
    @Operation(summary = "Concluir devolução rápida",
            description = "Registra o pedido já aprovado, conclui a devolução, tira o item do estoque "
                    + "e envia o recibo de retirada por e-mail.")
    public ResponseEntity<DevolucaoRapidaResponse> criar(@Valid @RequestBody ClaimCreateItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(claimService.criarDevolucaoRapida(request));
    }
}
