package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.EstadoCreateRequest;
import br.com.achadosperdidos.controller.dto.EstadoResponse;
import br.com.achadosperdidos.controller.dto.EstadoUpdateRequest;
import br.com.achadosperdidos.service.EstadoService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/estados")
@Tag(name = "Estados")
@SecurityRequirement(name = "bearerAuth")
public class EstadoController {
    private final EstadoService estadoService;
    public EstadoController(EstadoService estadoService) { this.estadoService = estadoService; }

    @GetMapping @PreAuthorize("@authz.pode('categoria.listar')")
    public List<EstadoResponse> findAll(@RequestParam(required = false, defaultValue = "false") boolean incluirInativos) {
        return estadoService.findAll(incluirInativos);
    }

    @PostMapping @PreAuthorize("@authz.pode('categoria.gerenciar')")
    public ResponseEntity<EstadoResponse> create(@Valid @RequestBody EstadoCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(estadoService.create(request));
    }

    @PutMapping("/{id}") @PreAuthorize("@authz.pode('categoria.gerenciar')")
    public EstadoResponse update(@PathVariable String id, @Valid @RequestBody EstadoUpdateRequest request) {
        return estadoService.update(id, request);
    }

    @DeleteMapping("/{id}") @PreAuthorize("@authz.pode('categoria.gerenciar')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        estadoService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
