package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.CategoriaCreateRequest;
import br.com.achadosperdidos.controller.dto.CategoriaResponse;
import br.com.achadosperdidos.controller.dto.CategoriaUpdateRequest;
import br.com.achadosperdidos.service.CategoriaService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/categorias")
@Tag(name = "Categorias")
@SecurityRequirement(name = "bearerAuth")
public class CategoriaController {
    private final CategoriaService categoriaService;
    public CategoriaController(CategoriaService categoriaService) { this.categoriaService = categoriaService; }

    @GetMapping @PreAuthorize("@authz.pode('categoria.listar')")
    public List<CategoriaResponse> findAll(@RequestParam(required = false, defaultValue = "false") boolean incluirInativos) {
        return categoriaService.findAll(incluirInativos);
    }

    @GetMapping("/{id}") @PreAuthorize("@authz.pode('categoria.listar')")
    public CategoriaResponse findById(@PathVariable String id) { return categoriaService.findById(id); }

    @PostMapping @PreAuthorize("@authz.pode('categoria.gerenciar')")
    public ResponseEntity<CategoriaResponse> create(@Valid @RequestBody CategoriaCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaService.create(request));
    }

    @PutMapping("/{id}") @PreAuthorize("@authz.pode('categoria.gerenciar')")
    public CategoriaResponse update(@PathVariable String id, @Valid @RequestBody CategoriaUpdateRequest request) {
        return categoriaService.update(id, request);
    }

    @DeleteMapping("/{id}") @PreAuthorize("@authz.pode('categoria.gerenciar')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        categoriaService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
