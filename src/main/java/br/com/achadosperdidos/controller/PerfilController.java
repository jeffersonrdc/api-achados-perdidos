package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.PerfilCreateRequest;
import br.com.achadosperdidos.controller.dto.PerfilDetalheResponse;
import br.com.achadosperdidos.controller.dto.PerfilResponse;
import br.com.achadosperdidos.controller.dto.PerfilUpdateRequest;
import br.com.achadosperdidos.controller.dto.PermissaoResponse;
import br.com.achadosperdidos.controller.dto.PermissoesRequest;
import br.com.achadosperdidos.service.PerfilService;
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
@RequestMapping("/api/v1/perfis")
@Tag(name = "Perfis", description = "Perfis de acesso e suas permissões.")
@SecurityRequirement(name = "bearerAuth")
public class PerfilController {
    private final PerfilService perfilService;

    public PerfilController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    @GetMapping
    @PreAuthorize("@authz.pode('perfil.listar')")
    @Operation(summary = "Listar perfis")
    public List<PerfilResponse> listar() {
        return perfilService.listar();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.pode('perfil.listar')")
    @Operation(summary = "Detalhar perfil")
    public PerfilDetalheResponse detalhe(
            @Parameter(description = "ID assinado do perfil") @PathVariable String id) {
        return perfilService.detalhe(id);
    }

    @PostMapping
    @PreAuthorize("@authz.pode('perfil.gerenciar')")
    @Operation(summary = "Criar perfil")
    public ResponseEntity<PerfilResponse> criar(@Valid @RequestBody PerfilCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(perfilService.criar(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.pode('perfil.gerenciar')")
    @Operation(summary = "Atualizar perfil")
    public PerfilResponse atualizar(
            @Parameter(description = "ID assinado do perfil") @PathVariable String id,
            @Valid @RequestBody PerfilUpdateRequest request) {
        return perfilService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.pode('perfil.gerenciar')")
    @Operation(summary = "Excluir perfil", description = "Exclusão lógica.")
    public ResponseEntity<Void> excluir(
            @Parameter(description = "ID assinado do perfil") @PathVariable String id) {
        perfilService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/permissoes")
    @PreAuthorize("@authz.pode('perfil.listar')")
    @Operation(summary = "Listar permissões do perfil")
    public List<PermissaoResponse> listarPermissoes(
            @Parameter(description = "ID assinado do perfil") @PathVariable String id) {
        return perfilService.listarPermissoes(id);
    }

    @PutMapping("/{id}/permissoes")
    @PreAuthorize("@authz.pode('perfil.gerenciar')")
    @Operation(summary = "Definir permissões do perfil",
            description = "Substitui o conjunto de permissões do perfil pelo informado no body.")
    public List<PermissaoResponse> definirPermissoes(
            @Parameter(description = "ID assinado do perfil") @PathVariable String id,
            @RequestBody PermissoesRequest request) {
        return perfilService.definirPermissoes(id, request);
    }
}
