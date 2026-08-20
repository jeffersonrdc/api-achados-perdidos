package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.FluxoConfigRequest;
import br.com.achadosperdidos.controller.dto.FluxoConfigResponse;
import br.com.achadosperdidos.service.FluxoConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/config/fluxo")
@Tag(name = "Configuração de Fluxo", description = "Fluxo global de triagem vs estoque direto.")
@SecurityRequirement(name = "bearerAuth")
public class FluxoConfigController {

    private final FluxoConfigService service;

    public FluxoConfigController(FluxoConfigService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("@authz.pode('configuracao.gerenciar')")
    @Operation(summary = "Obter configuração de fluxo")
    public FluxoConfigResponse obter() {
        return service.obter();
    }

    @PutMapping
    @PreAuthorize("@authz.pode('configuracao.gerenciar')")
    @Operation(summary = "Salvar configuração de fluxo")
    public FluxoConfigResponse salvar(@Valid @RequestBody FluxoConfigRequest request) {
        return service.salvar(request);
    }
}
