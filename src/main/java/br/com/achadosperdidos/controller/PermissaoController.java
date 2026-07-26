package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.PermissaoResponse;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.service.PermissaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/permissoes")
@Tag(name = "Permissões", description = "Catálogo de permissões do sistema.")
@SecurityRequirement(name = "bearerAuth")
public class PermissaoController {
    private final PermissaoService permissaoService;

    public PermissaoController(PermissaoService permissaoService) {
        this.permissaoService = permissaoService;
    }

    @GetMapping
    @PreAuthorize("@authz.pode('permissao.listar')")
    @Operation(summary = "Listar catálogo de permissões (paginado)",
            description = "Lista permissões disponíveis para atribuição a perfis e usuários.")
    public ApiPage<PermissaoResponse> listarCatalogo(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String q) {
        return permissaoService.listarCatalogo(page, limit, q);
    }
}
