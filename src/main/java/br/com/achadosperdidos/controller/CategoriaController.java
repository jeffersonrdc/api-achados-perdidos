package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.CategoriaResponse;
import br.com.achadosperdidos.service.CategoriaService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    public List<CategoriaResponse> findAll() { return categoriaService.findAll(); }
    @GetMapping("/{id}") @PreAuthorize("@authz.pode('categoria.listar')")
    public CategoriaResponse findById(@PathVariable String id) { return categoriaService.findById(id); }
}
