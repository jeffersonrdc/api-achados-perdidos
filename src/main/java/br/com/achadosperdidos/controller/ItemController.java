package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.ItemCreateRequest;
import br.com.achadosperdidos.controller.dto.ItemResponse;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.service.ItemService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/itens")
@Tag(name = "Itens")
@SecurityRequirement(name = "bearerAuth")
public class ItemController {
    private final ItemService itemService;
    public ItemController(ItemService itemService) { this.itemService = itemService; }
    @PostMapping @PreAuthorize("@authz.pode('item.criar')")
    public ResponseEntity<ItemResponse> create(@Valid @RequestBody ItemCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(itemService.create(request));
    }
    @GetMapping @PreAuthorize("@authz.pode('item.listar')")
    public ApiPage<ItemResponse> findAll(@RequestParam(required = false) Integer page,
                                         @RequestParam(required = false) Integer limit,
                                         @RequestParam(required = false) String idEvento) {
        return itemService.findAll(page, limit, idEvento);
    }
    @GetMapping("/{id}") @PreAuthorize("@authz.pode('item.listar')")
    public ItemResponse findById(@PathVariable String id) { return itemService.findById(id); }
    @DeleteMapping("/{id}") @PreAuthorize("@authz.pode('item.excluir')")
    public ResponseEntity<Void> delete(@PathVariable String id) { itemService.softDelete(id); return ResponseEntity.noContent().build(); }
}
