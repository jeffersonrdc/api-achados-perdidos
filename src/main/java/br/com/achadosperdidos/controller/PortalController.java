package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.*;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.security.PublicRateLimiter;
import br.com.achadosperdidos.service.PortalService;
import br.com.achadosperdidos.util.IpAddressUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/portal")
@Tag(name = "Portal do Participante",
        description = "Área pública do evento para consulta, claims, crianças e registro. "
                + "A maioria das rotas é pública; `meus-claims` exige JWT com ROLE_PARTICIPANTE.")
public class PortalController {

    private final PortalService portalService;
    private final PublicRateLimiter publicRateLimiter;

    public PortalController(PortalService portalService, PublicRateLimiter publicRateLimiter) {
        this.portalService = portalService;
        this.publicRateLimiter = publicRateLimiter;
    }

    private static String ipDe(HttpServletRequest http) {
        // getRemoteAddr() já reflete o IP real do cliente via Tomcat/RemoteIpValve
        // (server.forward-headers-strategy=NATIVE), sem confiar no XFF cru (anti-spoofing).
        return IpAddressUtil.normalize(http.getRemoteAddr());
    }

    @GetMapping("/eventos")
    @SecurityRequirements
    @Operation(summary = "Listar eventos abertos no portal",
            description = "Endpoint público. Retorna apenas eventos com portal habilitado.")
    public List<PortalEventoResumoResponse> listarEventos() {
        return portalService.listarEventosAbertos();
    }

    @GetMapping("/eventos/{idEvento}")
    @SecurityRequirements
    @Operation(summary = "Detalhar evento no portal")
    public PortalEventoResumoResponse detalharEvento(
            @Parameter(description = "ID assinado do evento (`s2.*`)") @PathVariable String idEvento) {
        return portalService.detalharEvento(idEvento);
    }

    @GetMapping("/categorias")
    @SecurityRequirements
    @Operation(summary = "Listar categorias públicas")
    public List<CategoriaResponse> listarCategorias() {
        return portalService.listarCategorias();
    }

    @GetMapping("/eventos/{idEvento}/locais")
    @SecurityRequirements
    @Operation(summary = "Listar locais do evento no portal")
    public List<PortalLocalResponse> listarLocais(
            @Parameter(description = "ID assinado do evento") @PathVariable String idEvento) {
        return portalService.listarLocais(idEvento);
    }

    @GetMapping("/eventos/{idEvento}/itens")
    @SecurityRequirements
    @Operation(summary = "Catálogo paginado de itens do evento",
            description = "Endpoint público. IDs de item na resposta são assinados.")
    public ApiPage<PortalItemCatalogoResponse> catalogoItens(
            @Parameter(description = "ID assinado do evento") @PathVariable String idEvento,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String pesquisa) {
        return portalService.catalogoItens(idEvento, page, limit, pesquisa);
    }

    @PostMapping("/eventos/{idEvento}/claims")
    @SecurityRequirements
    @Operation(summary = "Registrar objeto perdido (claim sem item vinculado)",
            description = "Endpoint público. Cria claim de objeto perdido informado pelo participante.")
    public ResponseEntity<ClaimResponse> registrarObjetoPerdido(
            @Parameter(description = "ID assinado do evento") @PathVariable String idEvento,
            @Valid @RequestBody PortalClaimCreateRequest request,
            HttpServletRequest http) {
        publicRateLimiter.check("portal-claim", ipDe(http));
        return ResponseEntity.status(HttpStatus.CREATED).body(portalService.registrarObjetoPerdido(idEvento, request));
    }

    @PostMapping("/eventos/{idEvento}/claims/item")
    @SecurityRequirements
    @Operation(summary = "Reclamar item específico do catálogo",
            description = "Endpoint público. Vincula claim a um item existente do evento.")
    public ResponseEntity<PortalClaimResultResponse> reclamarItem(
            @Parameter(description = "ID assinado do evento") @PathVariable String idEvento,
            @Valid @RequestBody PortalClaimItemRequest request,
            HttpServletRequest http) {
        publicRateLimiter.check("portal-claim-item", ipDe(http));
        return ResponseEntity.status(HttpStatus.CREATED).body(portalService.reclamarItem(idEvento, request));
    }

    @GetMapping("/eventos/{idEvento}/meus-claims")
    @PreAuthorize("hasRole('PARTICIPANTE')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Listar claims do participante autenticado",
            description = "Exige JWT com ROLE_PARTICIPANTE. O subject do token (e-mail) identifica o participante.")
    public List<ClaimResponse> meusClaims(
            @Parameter(description = "ID assinado do evento") @PathVariable String idEvento,
            Authentication auth) {
        return portalService.meusClaims(idEvento, auth.getName());
    }

    @PostMapping("/eventos/{idEvento}/criancas")
    @SecurityRequirements
    @Operation(summary = "Cadastrar criança no portal (público)")
    public ResponseEntity<CriancaResponse> cadastrarCrianca(
            @Parameter(description = "ID assinado do evento") @PathVariable String idEvento,
            @Valid @RequestBody CriancaCreateRequest request,
            HttpServletRequest http) {
        publicRateLimiter.check("portal-crianca", ipDe(http));
        return ResponseEntity.status(HttpStatus.CREATED).body(portalService.cadastrarCrianca(idEvento, request));
    }

    @PostMapping("/eventos/{idEvento}/criancas/responsaveis")
    @SecurityRequirements
    @Operation(summary = "Vincular responsável a uma criança (público)")
    public ResponseEntity<CriancaResponsavelResponse> vincularResponsavel(
            @Parameter(description = "ID assinado do evento") @PathVariable String idEvento,
            @Valid @RequestBody CriancaResponsavelCreateRequest request,
            HttpServletRequest http) {
        publicRateLimiter.check("portal-responsavel", ipDe(http));
        return ResponseEntity.status(HttpStatus.CREATED).body(portalService.vincularResponsavel(request));
    }

    @PostMapping("/auth/registro")
    @SecurityRequirements
    @Operation(summary = "Registrar participante do portal",
            description = "Endpoint público. Cria usuário com perfil de participante para autenticar e consultar seus claims.")
    public ResponseEntity<UsuarioResponse> registrarParticipante(
            @Valid @RequestBody PortalParticipanteRegisterRequest request,
            HttpServletRequest http) {
        publicRateLimiter.check("portal-registro", ipDe(http));
        return ResponseEntity.status(HttpStatus.CREATED).body(portalService.registrarParticipante(request));
    }
}
