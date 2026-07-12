package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.PermissaoResponse;
import br.com.achadosperdidos.service.PermissaoService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/permissoes")
@Tag(name = "Permissões")
@SecurityRequirement(name = "bearerAuth")
public class PermissaoController {
    private final PermissaoService permissaoService;

    public PermissaoController(PermissaoService permissaoService) {
        this.permissaoService = permissaoService;
    }

    @GetMapping
    @PreAuthorize("@authz.pode('permissao.listar')")
    public List<PermissaoResponse> listarCatalogo() {
        return permissaoService.listarCatalogo();
    }
}
