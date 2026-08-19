package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.ColetaFiltrosResponse;
import br.com.achadosperdidos.controller.dto.ColetaResumoResponse;
import br.com.achadosperdidos.controller.dto.EstoqueItemResponse;
import br.com.achadosperdidos.controller.dto.EstoqueResumoResponse;
import br.com.achadosperdidos.controller.dto.ItemClaimsResumoResponse;
import br.com.achadosperdidos.controller.dto.ItemCreateRequest;
import br.com.achadosperdidos.controller.dto.ItemLocalizacaoRequest;
import br.com.achadosperdidos.controller.dto.ItemResponse;
import br.com.achadosperdidos.controller.dto.ItemUpdateRequest;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.service.ClaimWorkflowService;
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
@RequestMapping("/api/v1/itens")
@Tag(name = "Itens", description = "Coleta, estoque, localização e exclusão lógica de achados.")
@SecurityRequirement(name = "bearerAuth")
public class ItemController {
    private final ItemService itemService;
    private final ClaimWorkflowService claimWorkflowService;
    public ItemController(ItemService itemService, ClaimWorkflowService claimWorkflowService) {
        this.itemService = itemService;
        this.claimWorkflowService = claimWorkflowService;
    }

    @PostMapping
    @PreAuthorize("@authz.pode('item.criar')")
    @Operation(summary = "Registrar item coletado (entra na fila de triagem)")
    public ResponseEntity<ItemResponse> create(@Valid @RequestBody ItemCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(itemService.create(request));
    }

    @GetMapping
    @PreAuthorize("@authz.pode('item.listar')")
    @Operation(summary = "Listar itens (coleta / filtros gerais)")
    public ApiPage<ItemResponse> findAll(@RequestParam(required = false) Integer page,
                                         @RequestParam(required = false) Integer limit,
                                         @Parameter(description = "ID assinado do evento") @RequestParam(required = false) String idEvento,
                                         @RequestParam(required = false) String q,
                                         @RequestParam(required = false) String idCategoria,
                                         @RequestParam(required = false) String local,
                                         @RequestParam(required = false) String tpPrioridade,
                                         @RequestParam(required = false) String status,
                                         @RequestParam(required = false) String data) {
        return itemService.findAll(page, limit, idEvento, q, idCategoria, local, tpPrioridade, status, data);
    }

    @GetMapping("/coleta/resumo")
    @PreAuthorize("@authz.pode('item.listar')")
    @Operation(summary = "Resumo da coleta do evento")
    public ColetaResumoResponse coletaResumo(
            @Parameter(description = "ID assinado do evento", required = true) @RequestParam String idEvento,
            @Parameter(description = "Data (yyyy-MM-dd ou dd/MM/yyyy); sem valor = totais do evento")
            @RequestParam(required = false) String data) {
        return itemService.coletaResumo(idEvento, data);
    }

    @GetMapping("/coleta/filtros")
    @PreAuthorize("@authz.pode('item.listar')")
    @Operation(summary = "Opções de filtro da tela de coleta")
    public ColetaFiltrosResponse coletaFiltros(
            @Parameter(description = "ID assinado do evento", required = true) @RequestParam String idEvento) {
        return itemService.coletaFiltros(idEvento);
    }

    @GetMapping("/estoque")
    @PreAuthorize("@authz.pode('item.listar')")
    @Operation(summary = "Listar itens em estoque")
    public ApiPage<EstoqueItemResponse> estoque(
            @Parameter(description = "ID assinado do evento", required = true) @RequestParam String idEvento,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String idCategoria,
            @RequestParam(required = false) String deposito,
            @RequestParam(required = false) String tpPrioridade,
            @RequestParam(required = false) String data,
            @Parameter(description = "Se true, oculta itens com pedido de retirada em aberto (devolução rápida).")
            @RequestParam(required = false, defaultValue = "false") boolean disponivelDevolucaoRapida) {
        if (disponivelDevolucaoRapida) {
            return itemService.listarEstoqueDevolucaoRapida(
                    idEvento, page, limit, q, idCategoria, deposito, tpPrioridade, data);
        }
        return itemService.listarEstoque(idEvento, page, limit, q, idCategoria, deposito, tpPrioridade, data);
    }

    @GetMapping("/estoque/resumo")
    @PreAuthorize("@authz.pode('item.listar')")
    @Operation(summary = "Resumo do estoque do evento")
    public EstoqueResumoResponse estoqueResumo(
            @Parameter(description = "ID assinado do evento", required = true) @RequestParam String idEvento,
            @Parameter(description = "Data (yyyy-MM-dd ou dd/MM/yyyy); sem valor = totais do evento")
            @RequestParam(required = false) String data) {
        return itemService.estoqueResumo(idEvento, data);
    }

    @GetMapping("/estoque/filtros")
    @PreAuthorize("@authz.pode('item.listar')")
    @Operation(summary = "Opções de filtro da tela de estoque")
    public ColetaFiltrosResponse estoqueFiltros(
            @Parameter(description = "ID assinado do evento", required = true) @RequestParam String idEvento) {
        return itemService.estoqueFiltros(idEvento);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.pode('item.listar')")
    @Operation(summary = "Buscar item por ID assinado")
    public ItemResponse findById(@PathVariable String id) { return itemService.findById(id); }

    @GetMapping("/{id}/claims-resumo")
    @PreAuthorize("@authz.pode('item.listar')")
    @Operation(summary = "Resumo de claims do item",
            description = "Usado na UI ao aprovar um pedido de devolução (quantidade de pedidos/reprovações).")
    public ItemClaimsResumoResponse claimsResumo(@PathVariable String id) {
        return claimWorkflowService.resumoItem(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.pode('item.editar')")
    @Operation(summary = "Atualizar dados do item")
    public ItemResponse update(@PathVariable String id, @RequestBody ItemUpdateRequest request) {
        return itemService.update(id, request);
    }

    @PutMapping("/{id}/localizacao")
    @PreAuthorize("@authz.pode('item.movimentar')")
    @Operation(summary = "Atualizar localização/estoque do item")
    public EstoqueItemResponse atualizarLocalizacao(@PathVariable String id, @Valid @RequestBody ItemLocalizacaoRequest request) {
        return itemService.atualizarLocalizacao(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.pode('item.excluir')")
    @Operation(summary = "Excluir item (soft delete)")
    public ResponseEntity<Void> delete(@PathVariable String id) { itemService.softDelete(id); return ResponseEntity.noContent().build(); }
}
