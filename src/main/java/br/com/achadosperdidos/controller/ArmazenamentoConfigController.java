package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.ArmazenamentoConfigRequest;
import br.com.achadosperdidos.controller.dto.ArmazenamentoConfigResponse;
import br.com.achadosperdidos.controller.dto.ArmazenamentoTesteResponse;
import br.com.achadosperdidos.service.ArmazenamentoConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/config/armazenamento")
@Tag(name = "Configuração de Armazenamento", description = "Provedor padrão Local ou AWS S3 para novos uploads.")
@SecurityRequirement(name = "bearerAuth")
public class ArmazenamentoConfigController {

    private final ArmazenamentoConfigService service;

    public ArmazenamentoConfigController(ArmazenamentoConfigService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("@authz.pode('configuracao.gerenciar')")
    @Operation(summary = "Obter configuração de armazenamento")
    public ArmazenamentoConfigResponse obter() {
        return service.obter();
    }

    @PutMapping
    @PreAuthorize("@authz.pode('configuracao.gerenciar')")
    @Operation(summary = "Definir provedor padrão (LOCAL ou S3)")
    public ArmazenamentoConfigResponse salvar(@Valid @RequestBody ArmazenamentoConfigRequest request) {
        return service.salvar(request);
    }

    @PostMapping("/teste")
    @PreAuthorize("@authz.pode('configuracao.gerenciar')")
    @Operation(summary = "Testar conexão com o provedor (padrão atual ou informado)")
    public ArmazenamentoTesteResponse testar(@RequestParam(required = false) String provider) {
        return service.testar(provider);
    }
}
