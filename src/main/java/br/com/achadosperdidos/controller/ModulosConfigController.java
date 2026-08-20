package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.ModulosConfigRequest;
import br.com.achadosperdidos.controller.dto.ModulosConfigResponse;
import br.com.achadosperdidos.service.ModulosConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/config/modulos")
@Tag(name = "Configuração de Módulos", description = "Habilita ou desabilita módulos do painel (antes da permissão).")
@SecurityRequirement(name = "bearerAuth")
public class ModulosConfigController {

    private final ModulosConfigService service;

    public ModulosConfigController(ModulosConfigService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("@authz.pode('configuracao.gerenciar')")
    @Operation(summary = "Obter toggles de módulos")
    public ModulosConfigResponse obter() {
        return service.obter();
    }

    @PutMapping
    @PreAuthorize("@authz.pode('configuracao.gerenciar')")
    @Operation(summary = "Salvar toggles de módulos")
    public ModulosConfigResponse salvar(@Valid @RequestBody ModulosConfigRequest request) {
        return service.salvar(request);
    }
}
