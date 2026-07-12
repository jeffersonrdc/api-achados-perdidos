package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.PerfilCreateRequest;
import br.com.achadosperdidos.controller.dto.PerfilDetalheResponse;
import br.com.achadosperdidos.controller.dto.PerfilResponse;
import br.com.achadosperdidos.controller.dto.PerfilUpdateRequest;
import br.com.achadosperdidos.controller.dto.PermissaoResponse;
import br.com.achadosperdidos.controller.dto.PermissoesRequest;
import br.com.achadosperdidos.service.PerfilService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/perfis")
@Tag(name = "Perfis")
@SecurityRequirement(name = "bearerAuth")
public class PerfilController {
    private final PerfilService perfilService;

    public PerfilController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    @GetMapping
    @PreAuthorize("@authz.pode('perfil.listar')")
    public List<PerfilResponse> listar() {
        return perfilService.listar();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.pode('perfil.listar')")
    public PerfilDetalheResponse detalhe(@PathVariable String id) {
        return perfilService.detalhe(id);
    }

    @PostMapping
    @PreAuthorize("@authz.pode('perfil.gerenciar')")
    public ResponseEntity<PerfilResponse> criar(@Valid @RequestBody PerfilCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(perfilService.criar(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.pode('perfil.gerenciar')")
    public PerfilResponse atualizar(@PathVariable String id, @Valid @RequestBody PerfilUpdateRequest request) {
        return perfilService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.pode('perfil.gerenciar')")
    public ResponseEntity<Void> excluir(@PathVariable String id) {
        perfilService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/permissoes")
    @PreAuthorize("@authz.pode('perfil.listar')")
    public List<PermissaoResponse> listarPermissoes(@PathVariable String id) {
        return perfilService.listarPermissoes(id);
    }

    @PutMapping("/{id}/permissoes")
    @PreAuthorize("@authz.pode('perfil.gerenciar')")
    public List<PermissaoResponse> definirPermissoes(@PathVariable String id, @RequestBody PermissoesRequest request) {
        return perfilService.definirPermissoes(id, request);
    }
}
