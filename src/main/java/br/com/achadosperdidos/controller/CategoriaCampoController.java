package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.CategoriaCampoCreateRequest;
import br.com.achadosperdidos.controller.dto.CategoriaCampoResponse;
import br.com.achadosperdidos.service.CategoriaCampoService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categorias/campos")
@Tag(name = "Campos de Categoria")
@SecurityRequirement(name = "bearerAuth")
public class CategoriaCampoController {
    private final CategoriaCampoService categoriaCampoService;

    public CategoriaCampoController(CategoriaCampoService categoriaCampoService) {
        this.categoriaCampoService = categoriaCampoService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoriaCampoResponse> create(@Valid @RequestBody CategoriaCampoCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaCampoService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR','ATENDENTE','CONSULTA')")
    public List<CategoriaCampoResponse> findByCategoria(@RequestParam String idCategoria) {
        return categoriaCampoService.findByCategoria(idCategoria);
    }
}
