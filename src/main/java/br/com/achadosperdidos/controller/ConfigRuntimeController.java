package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.ConfigRuntimeResponse;
import br.com.achadosperdidos.service.ConfigRuntimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/config/runtime")
@Tag(name = "Configuração Runtime", description = "Snapshot de módulos e fluxo para o painel autenticado.")
@SecurityRequirement(name = "bearerAuth")
public class ConfigRuntimeController {

    private final ConfigRuntimeService service;

    public ConfigRuntimeController(ConfigRuntimeService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obter runtime (módulos habilitados + fluxo)")
    public ConfigRuntimeResponse obter() {
        return service.obter();
    }
}
