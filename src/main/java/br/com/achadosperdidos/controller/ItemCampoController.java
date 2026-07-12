package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.ItemCampoResponse;
import br.com.achadosperdidos.controller.dto.ItemCampoUpsertRequest;
import br.com.achadosperdidos.service.ItemCampoService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/itens/campos")
@Tag(name = "Campos de Item")
@SecurityRequirement(name = "bearerAuth")
public class ItemCampoController {
    private final ItemCampoService itemCampoService;

    public ItemCampoController(ItemCampoService itemCampoService) {
        this.itemCampoService = itemCampoService;
    }

    @PutMapping
    @PreAuthorize("@authz.pode('item.campos')")
    public ResponseEntity<ItemCampoResponse> upsert(@Valid @RequestBody ItemCampoUpsertRequest request) {
        return ResponseEntity.ok(itemCampoService.upsert(request));
    }

    @GetMapping
    @PreAuthorize("@authz.pode('item.listar')")
    public List<ItemCampoResponse> findByItem(@RequestParam String idItem) {
        return itemCampoService.findByItem(idItem);
    }
}
