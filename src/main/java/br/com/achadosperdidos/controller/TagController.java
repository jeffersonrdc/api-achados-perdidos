package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.TagCreateRequest;
import br.com.achadosperdidos.controller.dto.TagResponse;
import br.com.achadosperdidos.controller.dto.TagUpdateRequest;
import br.com.achadosperdidos.service.TagService;
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
@RequestMapping("/api/v1/tags")
@Tag(name = "Tags", description = "Tags vinculadas a subcategorias para classificação de itens.")
@SecurityRequirement(name = "bearerAuth")
public class TagController {
    private final TagService tagService;
    public TagController(TagService tagService) { this.tagService = tagService; }

    @GetMapping
    @PreAuthorize("@authz.pode('categoria.listar')")
    @Operation(summary = "Listar tags",
            description = "Opcionalmente filtra por subcategoria.")
    public List<TagResponse> findAll(
            @Parameter(description = "Inclui tags inativas quando `true`")
            @RequestParam(required = false, defaultValue = "false") boolean incluirInativos,
            @Parameter(description = "ID assinado da subcategoria")
            @RequestParam(required = false) String idSubcategoria) {
        return tagService.findAll(incluirInativos, idSubcategoria);
    }

    @PostMapping
    @PreAuthorize("@authz.pode('categoria.gerenciar')")
    @Operation(summary = "Criar tag")
    public ResponseEntity<TagResponse> create(@Valid @RequestBody TagCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tagService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.pode('categoria.gerenciar')")
    @Operation(summary = "Atualizar tag")
    public TagResponse update(
            @Parameter(description = "ID assinado da tag") @PathVariable String id,
            @Valid @RequestBody TagUpdateRequest request) {
        return tagService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.pode('categoria.gerenciar')")
    @Operation(summary = "Excluir tag", description = "Exclusão lógica.")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID assinado da tag") @PathVariable String id) {
        tagService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
