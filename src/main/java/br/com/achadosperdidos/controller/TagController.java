package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.TagCreateRequest;
import br.com.achadosperdidos.controller.dto.TagResponse;
import br.com.achadosperdidos.controller.dto.TagUpdateRequest;
import br.com.achadosperdidos.service.TagService;
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
@Tag(name = "Tags")
@SecurityRequirement(name = "bearerAuth")
public class TagController {
    private final TagService tagService;
    public TagController(TagService tagService) { this.tagService = tagService; }

    @GetMapping @PreAuthorize("@authz.pode('categoria.listar')")
    public List<TagResponse> findAll(
            @RequestParam(required = false, defaultValue = "false") boolean incluirInativos,
            @RequestParam(required = false) String idSubcategoria) {
        return tagService.findAll(incluirInativos, idSubcategoria);
    }

    @PostMapping @PreAuthorize("@authz.pode('categoria.gerenciar')")
    public ResponseEntity<TagResponse> create(@Valid @RequestBody TagCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tagService.create(request));
    }

    @PutMapping("/{id}") @PreAuthorize("@authz.pode('categoria.gerenciar')")
    public TagResponse update(@PathVariable String id, @Valid @RequestBody TagUpdateRequest request) {
        return tagService.update(id, request);
    }

    @DeleteMapping("/{id}") @PreAuthorize("@authz.pode('categoria.gerenciar')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        tagService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
