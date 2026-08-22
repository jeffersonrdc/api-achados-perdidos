package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.PortalDevolucaoContextResponse;
import br.com.achadosperdidos.controller.dto.PortalDevolucaoModalidadeRequest;
import br.com.achadosperdidos.controller.dto.PortalDevolucaoPickupConfirmRequest;
import br.com.achadosperdidos.controller.dto.PortalDevolucaoShippingAddressRequest;
import br.com.achadosperdidos.security.PublicRateLimiter;
import br.com.achadosperdidos.service.DevolucaoFluxoService;
import br.com.achadosperdidos.util.ClientIpResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/portal/devolucoes")
@Tag(name = "Portal — Devoluções", description = "Magic links públicos do fluxo PICKUP/SHIPPING.")
@SecurityRequirements
public class PortalDevolucaoController {

    private final DevolucaoFluxoService devolucaoFluxoService;
    private final PublicRateLimiter publicRateLimiter;
    private final ClientIpResolver clientIpResolver;

    public PortalDevolucaoController(DevolucaoFluxoService devolucaoFluxoService,
                                     PublicRateLimiter publicRateLimiter,
                                     ClientIpResolver clientIpResolver) {
        this.devolucaoFluxoService = devolucaoFluxoService;
        this.publicRateLimiter = publicRateLimiter;
        this.clientIpResolver = clientIpResolver;
    }

    @GetMapping("/{token}")
    @Operation(summary = "Contexto do token de devolução")
    public PortalDevolucaoContextResponse contexto(@PathVariable String token, HttpServletRequest http) {
        publicRateLimiter.check("portal-devolucao-get", ipDe(http));
        return devolucaoFluxoService.contexto(token);
    }

    @PostMapping("/{token}/modalidade")
    @Operation(summary = "Escolher modalidade PICKUP ou SHIPPING")
    public Map<String, Object> modalidade(@PathVariable String token,
                                          @Valid @RequestBody PortalDevolucaoModalidadeRequest request,
                                          HttpServletRequest http) {
        publicRateLimiter.check("portal-devolucao-post", ipDe(http));
        return devolucaoFluxoService.modalidade(token, request);
    }

    @PostMapping("/{token}/pickup/request")
    @Operation(summary = "Solicitar opções de agendamento")
    public Map<String, Object> pickupRequest(@PathVariable String token, HttpServletRequest http) {
        publicRateLimiter.check("portal-devolucao-post", ipDe(http));
        return devolucaoFluxoService.pickupRequest(token);
    }

    @PostMapping("/{token}/pickup/confirm")
    @Operation(summary = "Confirmar opção de agenda")
    public Map<String, Object> pickupConfirm(@PathVariable String token,
                                             @Valid @RequestBody PortalDevolucaoPickupConfirmRequest request,
                                             HttpServletRequest http) {
        publicRateLimiter.check("portal-devolucao-post", ipDe(http));
        return devolucaoFluxoService.pickupConfirm(token, request);
    }

    @PostMapping("/{token}/shipping/address")
    @Operation(summary = "Enviar endereço para Correios")
    public Map<String, Object> shippingAddress(@PathVariable String token,
                                               @Valid @RequestBody PortalDevolucaoShippingAddressRequest request,
                                               HttpServletRequest http) {
        publicRateLimiter.check("portal-devolucao-post", ipDe(http));
        return devolucaoFluxoService.shippingAddress(token, request);
    }

    @PostMapping(value = "/{token}/shipping/payment-proof", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Enviar comprovante de pagamento do frete")
    public Map<String, Object> paymentProof(@PathVariable String token,
                                            @RequestParam("comprovante") MultipartFile comprovante,
                                            HttpServletRequest http) {
        publicRateLimiter.check("portal-devolucao-post", ipDe(http));
        return devolucaoFluxoService.paymentProof(token, comprovante);
    }

    @GetMapping("/{token}/tracking")
    @Operation(summary = "Consultar rastreio")
    public PortalDevolucaoContextResponse.Tracking tracking(@PathVariable String token, HttpServletRequest http) {
        publicRateLimiter.check("portal-devolucao-get", ipDe(http));
        return devolucaoFluxoService.tracking(token);
    }

    private String ipDe(HttpServletRequest http) {
        return clientIpResolver.resolve(http);
    }
}
