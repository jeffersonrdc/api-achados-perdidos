package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.PermissaoResponse;
import br.com.achadosperdidos.controller.dto.PermissoesRequest;
import br.com.achadosperdidos.controller.dto.UsuarioCreateRequest;
import br.com.achadosperdidos.controller.dto.UsuarioResponse;
import br.com.achadosperdidos.controller.dto.UsuarioUpdateRequest;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.service.UsuarioPermissaoService;
import br.com.achadosperdidos.service.UsuarioService;
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
@RequestMapping("/api/v1/usuarios")
@Tag(name = "Usuários", description = "Gestão de usuários e permissões extras.")
@SecurityRequirement(name = "bearerAuth")
public class UsuarioController {
    private final UsuarioService usuarioService;
    private final UsuarioPermissaoService usuarioPermissaoService;

    public UsuarioController(UsuarioService usuarioService, UsuarioPermissaoService usuarioPermissaoService) {
        this.usuarioService = usuarioService;
        this.usuarioPermissaoService = usuarioPermissaoService;
    }

    @PostMapping
    @PreAuthorize("@authz.pode('usuario.criar')")
    @Operation(summary = "Criar usuário")
    public ResponseEntity<UsuarioResponse> create(@Valid @RequestBody UsuarioCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.create(request));
    }

    @GetMapping
    @PreAuthorize("@authz.pode('usuario.listar')")
    @Operation(summary = "Listar usuários (paginado)")
    public ApiPage<UsuarioResponse> findAll(@RequestParam(required = false) Integer page,
                                            @RequestParam(required = false) Integer limit) {
        return usuarioService.findAll(page, limit);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.pode('usuario.listar')")
    @Operation(summary = "Buscar usuário por ID assinado")
    public UsuarioResponse findById(@Parameter(description = "ID assinado do usuário") @PathVariable String id) {
        return usuarioService.findById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.pode('usuario.editar')")
    @Operation(summary = "Atualizar usuário")
    public UsuarioResponse update(@Parameter(description = "ID assinado do usuário") @PathVariable String id, @Valid @RequestBody UsuarioUpdateRequest request) {
        return usuarioService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.pode('usuario.excluir')")
    @Operation(summary = "Excluir usuário (soft delete)")
    public ResponseEntity<Void> delete(@Parameter(description = "ID assinado do usuário") @PathVariable String id) {
        usuarioService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/permissoes")
    @PreAuthorize("@authz.pode('usuario.permissoes')")
    @Operation(summary = "Listar permissões extras do usuário")
    public List<PermissaoResponse> listarPermissoesExtras(@Parameter(description = "ID assinado do usuário") @PathVariable String id) {
        return usuarioPermissaoService.listarExtras(id);
    }

    @PutMapping("/{id}/permissoes")
    @PreAuthorize("@authz.pode('usuario.permissoes')")
    @Operation(summary = "Definir permissões extras do usuário")
    public List<PermissaoResponse> definirPermissoesExtras(@Parameter(description = "ID assinado do usuário") @PathVariable String id, @RequestBody PermissoesRequest request) {
        return usuarioPermissaoService.definirExtras(id, request);
    }

    @GetMapping("/{id}/permissoes-efetivas")
    @PreAuthorize("@authz.pode('usuario.permissoes')")
    @Operation(summary = "Listar permissões efetivas do usuário", description = "Perfil + extras aplicados.")
    public List<String> permissoesEfetivas(@Parameter(description = "ID assinado do usuário") @PathVariable String id) {
        return usuarioPermissaoService.efetivas(id);
    }
}
