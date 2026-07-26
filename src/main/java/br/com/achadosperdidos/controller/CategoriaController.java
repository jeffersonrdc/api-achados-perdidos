package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.CategoriaCreateRequest;
import br.com.achadosperdidos.controller.dto.CategoriaResponse;
import br.com.achadosperdidos.controller.dto.CategoriaUpdateRequest;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.service.CategoriaService;
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
@RequestMapping("/api/v1/categorias")
@Tag(name = "Categorias", description = "Categorias-pai e subcategorias do catálogo de itens.")
@SecurityRequirement(name = "bearerAuth")
public class CategoriaController {
    private final CategoriaService categoriaService;
    public CategoriaController(CategoriaService categoriaService) { this.categoriaService = categoriaService; }

    @GetMapping
    @PreAuthorize("@authz.pode('categoria.listar')")
    @Operation(summary = "Listar categorias raiz ou filhos (paginado)",
            description = "Sem `idPai` retorna apenas categorias-pai. Com `idPai` retorna as subcategorias desse pai.")
    public ApiPage<CategoriaResponse> findAll(
            @Parameter(description = "Inclui categorias inativas quando `true`")
            @RequestParam(required = false, defaultValue = "false") boolean incluirInativos,
            @Parameter(description = "ID assinado da categoria-pai; quando informado, lista filhos")
            @RequestParam(required = false) String idPai,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String q) {
        return categoriaService.findAll(incluirInativos, idPai, page, limit, q);
    }

    @GetMapping("/subcategorias")
    @PreAuthorize("@authz.pode('categoria.listar')")
    @Operation(summary = "Listar todas as subcategorias (paginado)",
            description = "Retorna subcategorias de qualquer pai — usado na tela /caracteristicas.")
    public ApiPage<CategoriaResponse> findAllSubcategorias(
            @Parameter(description = "Inclui subcategorias inativas quando `true`")
            @RequestParam(required = false, defaultValue = "false") boolean incluirInativos,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String q) {
        return categoriaService.findAllSubcategorias(incluirInativos, page, limit, q);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.pode('categoria.listar')")
    @Operation(summary = "Detalhar categoria")
    public CategoriaResponse findById(
            @Parameter(description = "ID assinado da categoria") @PathVariable String id) {
        return categoriaService.findById(id);
    }

    @PostMapping
    @PreAuthorize("@authz.pode('categoria.gerenciar')")
    @Operation(summary = "Criar categoria ou subcategoria",
            description = "Informe `idPai` no body para criar subcategoria; omita para categoria-raiz.")
    public ResponseEntity<CategoriaResponse> create(@Valid @RequestBody CategoriaCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.pode('categoria.gerenciar')")
    @Operation(summary = "Atualizar categoria")
    public CategoriaResponse update(
            @Parameter(description = "ID assinado da categoria") @PathVariable String id,
            @Valid @RequestBody CategoriaUpdateRequest request) {
        return categoriaService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.pode('categoria.gerenciar')")
    @Operation(summary = "Excluir categoria", description = "Exclusão lógica.")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID assinado da categoria") @PathVariable String id) {
        categoriaService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
