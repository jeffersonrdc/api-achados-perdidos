package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.PortalContatosConfigRequest;
import br.com.achadosperdidos.controller.dto.PortalContatosConfigResponse;
import br.com.achadosperdidos.service.PortalContatosConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/config/portal-contatos")
@Tag(name = "Configuração de Contatos do Portal",
        description = "Telefone, WhatsApp e e-mail exibidos na página /contato do portal público.")
@SecurityRequirement(name = "bearerAuth")
public class PortalContatosConfigController {

    private final PortalContatosConfigService service;

    public PortalContatosConfigController(PortalContatosConfigService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("@authz.pode('configuracao.gerenciar')")
    @Operation(summary = "Obter canais de contato do portal")
    public PortalContatosConfigResponse obter() {
        return service.obter();
    }

    @PutMapping
    @PreAuthorize("@authz.pode('configuracao.gerenciar')")
    @Operation(summary = "Salvar canais de contato do portal")
    public PortalContatosConfigResponse salvar(@Valid @RequestBody PortalContatosConfigRequest request) {
        return service.salvar(request);
    }
}
