package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.UsuarioCreateRequest;
import br.com.achadosperdidos.controller.dto.UsuarioResponse;
import br.com.achadosperdidos.controller.dto.UsuarioUpdateRequest;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.service.UsuarioService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/usuarios")
@Tag(name = "Usuários")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {
    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> create(@Valid @RequestBody UsuarioCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.create(request));
    }

    @GetMapping
    public ApiPage<UsuarioResponse> findAll(@RequestParam(required = false) Integer page,
                                            @RequestParam(required = false) Integer limit) {
        return usuarioService.findAll(page, limit);
    }

    @GetMapping("/{id}")
    public UsuarioResponse findById(@PathVariable String id) {
        return usuarioService.findById(id);
    }

    @PutMapping("/{id}")
    public UsuarioResponse update(@PathVariable String id, @Valid @RequestBody UsuarioUpdateRequest request) {
        return usuarioService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        usuarioService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
