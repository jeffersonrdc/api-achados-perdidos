package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.EmailConfigRequest;
import br.com.achadosperdidos.controller.dto.EmailConfigResponse;
import br.com.achadosperdidos.controller.dto.EmailParametroResponse;
import br.com.achadosperdidos.controller.dto.EmailParametroUpdateRequest;
import br.com.achadosperdidos.service.ClaimConfigService;
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

/** Configuração de e-mail (contas SMTP + parâmetros de envio) exibida na tela /configuracoes. */
@RestController
@RequestMapping("/api/v1/config/email")
@Tag(name = "Configuração de E-mail", description = "Contas SMTP e parâmetros de envio de e-mail.")
@SecurityRequirement(name = "bearerAuth")
public class EmailConfigController {
    private final ClaimConfigService claimConfigService;
    public EmailConfigController(ClaimConfigService claimConfigService) { this.claimConfigService = claimConfigService; }

    @GetMapping @PreAuthorize("@authz.pode('configuracao.gerenciar')")
    @Operation(summary = "Listar contas de e-mail")
    public List<EmailConfigResponse> listar() { return claimConfigService.listarConfigs(); }

    @PostMapping @PreAuthorize("@authz.pode('configuracao.gerenciar')")
    @Operation(summary = "Criar conta de e-mail")
    public ResponseEntity<EmailConfigResponse> criar(@Valid @RequestBody EmailConfigRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(claimConfigService.criarConfig(request));
    }

    @PutMapping("/{id}") @PreAuthorize("@authz.pode('configuracao.gerenciar')")
    @Operation(summary = "Atualizar conta de e-mail")
    public EmailConfigResponse atualizar(@Parameter(description = "ID assinado da configuração") @PathVariable String id, @Valid @RequestBody EmailConfigRequest request) {
        return claimConfigService.atualizarConfig(id, request);
    }

    @DeleteMapping("/{id}") @PreAuthorize("@authz.pode('configuracao.gerenciar')")
    @Operation(summary = "Excluir conta de e-mail")
    public ResponseEntity<Void> excluir(@Parameter(description = "ID assinado da configuração") @PathVariable String id) {
        claimConfigService.excluirConfig(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/parametros") @PreAuthorize("@authz.pode('configuracao.gerenciar')")
    @Operation(summary = "Listar parâmetros de envio")
    public List<EmailParametroResponse> parametros() { return claimConfigService.listarParametros(); }

    @PutMapping("/parametros") @PreAuthorize("@authz.pode('configuracao.gerenciar')")
    @Operation(summary = "Salvar parâmetros de envio")
    public List<EmailParametroResponse> salvarParametros(@Valid @RequestBody List<EmailParametroUpdateRequest> request) {
        return claimConfigService.salvarParametros(request);
    }
}
