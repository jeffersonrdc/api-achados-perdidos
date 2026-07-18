package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.*;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.security.PublicRateLimiter;
import br.com.achadosperdidos.service.ClaimMensagemService;
import br.com.achadosperdidos.service.PortalService;
import br.com.achadosperdidos.util.IpAddressUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
    private final ClaimMensagemService claimMensagemService;
    private final PublicRateLimiter publicRateLimiter;

    public PortalController(PortalService portalService, ClaimMensagemService claimMensagemService,
                            PublicRateLimiter publicRateLimiter) {
        this.portalService = portalService;
        this.claimMensagemService = claimMensagemService;
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
    @Operation(summary = "Listar categorias públicas (apenas categorias-pai)")
    public List<CategoriaResponse> listarCategorias() {
        return portalService.listarCategorias();
    }

    @GetMapping("/categorias/{idCategoria}/subcategorias")
    @SecurityRequirements
    @Operation(summary = "Listar subcategorias públicas de uma categoria")
    public List<CategoriaResponse> listarSubcategorias(
            @Parameter(description = "ID assinado da categoria-pai") @PathVariable String idCategoria) {
        return portalService.listarSubcategorias(idCategoria);
    }

    @GetMapping("/subcategorias/{idSubcategoria}/tags")
    @SecurityRequirements
    @Operation(summary = "Listar tags públicas de uma subcategoria")
    public List<br.com.achadosperdidos.controller.dto.TagResponse> listarTags(
            @Parameter(description = "ID assinado da subcategoria") @PathVariable String idSubcategoria) {
        return portalService.listarTags(idSubcategoria);
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

    @GetMapping("/arquivos/{idArquivo}/download")
    @SecurityRequirements
    @Operation(summary = "Baixar foto pública de item do catálogo",
            description = "Endpoint público. Somente foto principal de item visível no portal. Streaming Local/S3.")
    public ResponseEntity<Resource> baixarFotoPublica(
            @Parameter(description = "ID assinado do arquivo") @PathVariable String idArquivo,
            HttpServletRequest http) {
        publicRateLimiter.check("portal-foto", ipDe(http));
        var conteudo = portalService.baixarFotoPublica(idArquivo);
        MediaType mime = conteudo.tpMime() != null
                ? MediaType.parseMediaType(conteudo.tpMime())
                : MediaType.IMAGE_JPEG;
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .contentType(mime)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=300");
        if (conteudo.qtBytes() != null && conteudo.qtBytes() >= 0) {
            builder.contentLength(conteudo.qtBytes());
        }
        return builder.body(conteudo.resource());
    }

    @PostMapping("/eventos/{idEvento}/claims")
    @SecurityRequirements
    @Operation(summary = "Registrar objeto perdido (claim PERDA)",
            description = "Endpoint público. Cria relato de perda informado pelo participante.")
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
            description = "Endpoint público. Vincula claim (RETIRADA) a um item existente do evento.")
    public ResponseEntity<PortalClaimResultResponse> reclamarItem(
            @Parameter(description = "ID assinado do evento") @PathVariable String idEvento,
            @Valid @RequestBody PortalClaimItemRequest request,
            HttpServletRequest http) {
        publicRateLimiter.check("portal-claim-item", ipDe(http));
        return ResponseEntity.status(HttpStatus.CREATED).body(portalService.reclamarItem(idEvento, request));
    }

    @PostMapping(
            value = "/eventos/{idEvento}/claims/{idClaim}/comprovantes",
            consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @SecurityRequirements
    @Operation(summary = "Anexar comprovantes à solicitação de retirada",
            description = "Endpoint público. Até 5 arquivos PDF/JPEG/PNG de 10 MB, vinculados ao claim RETIRADA.")
    public ResponseEntity<List<ArquivoResponse>> uploadComprovantesRetirada(
            @Parameter(description = "ID assinado do evento") @PathVariable String idEvento,
            @Parameter(description = "ID assinado do claim de retirada") @PathVariable String idClaim,
            @RequestParam("anexos") List<org.springframework.web.multipart.MultipartFile> anexos,
            HttpServletRequest http) {
        publicRateLimiter.check("portal-claim-comprovantes", ipDe(http));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(portalService.uploadComprovantesRetirada(idEvento, idClaim, anexos));
    }

    @PostMapping(value = "/eventos/{idEvento}/claims/{idClaim}/foto", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @SecurityRequirements
    @Operation(summary = "Anexar foto ao relato de perda",
            description = "Endpoint público. JPEG/PNG até 5 MB. Somente claims do tipo PERDA.")
    public ResponseEntity<ArquivoResponse> uploadFotoClaim(
            @Parameter(description = "ID assinado do evento") @PathVariable String idEvento,
            @Parameter(description = "ID assinado do claim") @PathVariable String idClaim,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            HttpServletRequest http) {
        publicRateLimiter.check("portal-claim-foto", ipDe(http));
        return ResponseEntity.status(HttpStatus.CREATED).body(portalService.uploadFotoClaim(idEvento, idClaim, file));
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

    @GetMapping("/respostas/{token}")
    @SecurityRequirements
    @Operation(summary = "Contexto público do link de resposta",
            description = "Valida o token do e-mail e retorna dados mínimos (sem PII sensível).")
    public PortalRespostaContextResponse contextoResposta(
            @PathVariable String token,
            HttpServletRequest http) {
        publicRateLimiter.check("portal-resposta-get", ipDe(http));
        return claimMensagemService.contextoPublico(token);
    }

    @PostMapping(value = "/respostas/{token}", consumes = {
            org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE,
            org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE
    })
    @SecurityRequirements
    @Operation(summary = "Enviar resposta pública pelo link do e-mail",
            description = "Texto obrigatório; imagens JPEG/PNG opcionais (até 5 arquivos de 5 MB).")
    public PortalRespostaSubmitResponse enviarResposta(
            @PathVariable String token,
            @RequestParam("dsMensagem") String dsMensagem,
            @RequestParam(value = "imagens", required = false) List<org.springframework.web.multipart.MultipartFile> imagens,
            HttpServletRequest http) {
        publicRateLimiter.check("portal-resposta-post", ipDe(http));
        return claimMensagemService.responderPublico(token, dsMensagem, imagens);
    }
}
