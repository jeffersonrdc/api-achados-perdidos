package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.*;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.security.PublicRateLimiter;
import br.com.achadosperdidos.service.ClaimMensagemService;
import br.com.achadosperdidos.service.PortalService;
import br.com.achadosperdidos.util.IpAddressUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
                + "A maioria das rotas é pública com rate limit por IP; "
                + "`meus-claims` exige JWT com ROLE_PARTICIPANTE. "
                + "IDs de path/query são tokens assinados (`s2.*`), nunca numéricos sequenciais.")
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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de eventos disponíveis")
    })
    public List<PortalEventoResumoResponse> listarEventos() {
        return portalService.listarEventosAbertos();
    }

    @GetMapping("/status")
    @SecurityRequirements
    @Operation(summary = "Status de liberação do portal público",
            description = "Indica se o portal já pode ser acessado com base na data/hora de início do evento.")
    public PortalStatusResponse statusPortal() {
        return portalService.statusPortal();
    }

    @GetMapping("/eventos/{idEvento}")
    @SecurityRequirements
    @Operation(summary = "Detalhar evento no portal",
            description = "Endpoint público. Retorna 404 se o evento não existir ou o portal estiver desabilitado.")
    public PortalEventoResumoResponse detalharEvento(
            @Parameter(description = "ID assinado do evento (`s2.*`)", required = true)
            @PathVariable String idEvento) {
        return portalService.detalharEvento(idEvento);
    }

    @GetMapping("/categorias")
    @SecurityRequirements
    @Operation(summary = "Listar categorias públicas (apenas categorias-pai)",
            description = "Endpoint público. Usado nos filtros do catálogo do portal.")
    public List<CategoriaResponse> listarCategorias() {
        return portalService.listarCategorias();
    }

    @GetMapping("/categorias/{idCategoria}/subcategorias")
    @SecurityRequirements
    @Operation(summary = "Listar subcategorias públicas de uma categoria")
    public List<CategoriaResponse> listarSubcategorias(
            @Parameter(description = "ID assinado da categoria-pai", required = true)
            @PathVariable String idCategoria) {
        return portalService.listarSubcategorias(idCategoria);
    }

    @GetMapping("/subcategorias/{idSubcategoria}/tags")
    @SecurityRequirements
    @Operation(summary = "Listar tags públicas de uma subcategoria")
    public List<br.com.achadosperdidos.controller.dto.TagResponse> listarTags(
            @Parameter(description = "ID assinado da subcategoria", required = true)
            @PathVariable String idSubcategoria) {
        return portalService.listarTags(idSubcategoria);
    }

    @GetMapping("/eventos/{idEvento}/locais")
    @SecurityRequirements
    @Operation(summary = "Listar locais do evento no portal")
    public List<PortalLocalResponse> listarLocais(
            @Parameter(description = "ID assinado do evento", required = true) @PathVariable String idEvento) {
        return portalService.listarLocais(idEvento);
    }

    @GetMapping("/eventos/{idEvento}/itens")
    @SecurityRequirements
    @Operation(summary = "Catálogo paginado de itens do evento",
            description = "Endpoint público. Retorna itens visíveis no portal com IDs assinados. "
                    + "Paginação baseada em `page` (1+) e `limit`.")
    public ApiPage<PortalItemCatalogoResponse> catalogoItens(
            @Parameter(description = "ID assinado do evento", required = true) @PathVariable String idEvento,
            @Parameter(description = "Página baseada em 1") @RequestParam(required = false) Integer page,
            @Parameter(description = "Quantidade por página") @RequestParam(required = false) Integer limit,
            @Parameter(description = "Texto livre de busca") @RequestParam(required = false) String pesquisa) {
        return portalService.catalogoItens(idEvento, page, limit, pesquisa);
    }

    @GetMapping("/eventos/{idEvento}/itens/{idItem}")
    @SecurityRequirements
    @Operation(summary = "Detalhe público de um item do catálogo",
            description = "Retorna todos os dados públicos do item (descrição, protocolo, foto, etc.).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item encontrado"),
            @ApiResponse(responseCode = "404", description = "Item inexistente ou não público")
    })
    public PortalItemDetalheResponse detalharItem(
            @Parameter(description = "ID assinado do evento", required = true) @PathVariable String idEvento,
            @Parameter(description = "ID assinado do item", required = true) @PathVariable String idItem) {
        return portalService.detalharItem(idEvento, idItem);
    }

    @GetMapping("/arquivos/{idArquivo}/download")
    @SecurityRequirements
    @Operation(summary = "Baixar foto pública de item do catálogo",
            description = "Endpoint público com rate limit por IP. "
                    + "Somente foto principal de item visível no portal. Streaming Local/S3. "
                    + "Resposta binária com Content-Type da imagem.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stream da imagem",
                    content = @Content(mediaType = "image/jpeg",
                            schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "404", description = "Arquivo inexistente ou não público"),
            @ApiResponse(responseCode = "429", description = "Rate limit excedido")
    })
    public ResponseEntity<Resource> baixarFotoPublica(
            @Parameter(description = "ID assinado do arquivo", required = true) @PathVariable String idArquivo,
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

    @GetMapping("/arquivos/{idArquivo}/thumbnail")
    @SecurityRequirements
    @Operation(summary = "Miniatura da foto pública do catálogo",
            description = "Endpoint público com rate limit por IP. "
                    + "Retorna JPEG redimensionado (padrão max 400px no maior lado) para cards/listagens. "
                    + "Query `max` opcional (64–800).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stream da miniatura JPEG",
                    content = @Content(mediaType = "image/jpeg",
                            schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "404", description = "Arquivo inexistente ou não público"),
            @ApiResponse(responseCode = "429", description = "Rate limit excedido")
    })
    public ResponseEntity<Resource> baixarThumbnailPublica(
            @Parameter(description = "ID assinado do arquivo", required = true) @PathVariable String idArquivo,
            @Parameter(description = "Maior lado em pixels (padrão 400, máx. 800)")
            @RequestParam(required = false) Integer max,
            HttpServletRequest http) {
        publicRateLimiter.check("portal-foto", ipDe(http));
        var conteudo = portalService.baixarThumbnailPublica(idArquivo, max);
        MediaType mime = conteudo.tpMime() != null
                ? MediaType.parseMediaType(conteudo.tpMime())
                : MediaType.IMAGE_JPEG;
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .contentType(mime)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400");
        if (conteudo.qtBytes() != null && conteudo.qtBytes() >= 0) {
            builder.contentLength(conteudo.qtBytes());
        }
        return builder.body(conteudo.resource());
    }

    @PostMapping("/eventos/{idEvento}/claims")
    @SecurityRequirements
    @Operation(summary = "Registrar objeto perdido (claim PERDA)",
            description = "Endpoint público com rate limit por IP. Cria relato de perda informado pelo participante.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Claim criado"),
            @ApiResponse(responseCode = "429", description = "Rate limit excedido")
    })
    public ResponseEntity<ClaimResponse> registrarObjetoPerdido(
            @Parameter(description = "ID assinado do evento", required = true) @PathVariable String idEvento,
            @Valid @RequestBody PortalClaimCreateRequest request,
            HttpServletRequest http) {
        publicRateLimiter.check("portal-claim", ipDe(http));
        return ResponseEntity.status(HttpStatus.CREATED).body(portalService.registrarObjetoPerdido(idEvento, request));
    }

    @PostMapping("/eventos/{idEvento}/claims/item")
    @SecurityRequirements
    @Operation(summary = "Reclamar item específico do catálogo",
            description = "Endpoint público com rate limit por IP. "
                    + "Cria claim do tipo RETIRADA vinculado a um item existente e elegível do evento. "
                    + "Após o sucesso, anexe comprovantes em `POST .../claims/{idClaim}/comprovantes`.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Solicitação de retirada criada"),
            @ApiResponse(responseCode = "409", description = "Item indisponível para reclamação"),
            @ApiResponse(responseCode = "429", description = "Rate limit excedido")
    })
    public ResponseEntity<PortalClaimResultResponse> reclamarItem(
            @Parameter(description = "ID assinado do evento", required = true) @PathVariable String idEvento,
            @Valid @RequestBody PortalClaimItemRequest request,
            HttpServletRequest http) {
        publicRateLimiter.check("portal-claim-item", ipDe(http));
        return ResponseEntity.status(HttpStatus.CREATED).body(portalService.reclamarItem(idEvento, request));
    }

    @PostMapping(
            value = "/eventos/{idEvento}/claims/{idClaim}/comprovantes",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SecurityRequirements
    @Operation(summary = "Anexar comprovantes à solicitação de retirada",
            description = "Endpoint público com rate limit por IP. "
                    + "Campo multipart `anexos`: até 5 arquivos PDF/JPEG/PNG de no máximo 10 MB cada. "
                    + "Vincula ao claim RETIRADA (`TP_Entidade=CLAIM`, `TP_Arquivo=COMPROVANTE`). "
                    + "Pode ser chamado novamente se o upload anterior falhou, sem criar novo claim.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Comprovantes armazenados"),
            @ApiResponse(responseCode = "400", description = "Arquivo inválido ou limites excedidos"),
            @ApiResponse(responseCode = "413", description = "Payload acima do limite"),
            @ApiResponse(responseCode = "415", description = "MIME não suportado"),
            @ApiResponse(responseCode = "429", description = "Rate limit excedido")
    })
    public ResponseEntity<List<ArquivoResponse>> uploadComprovantesRetirada(
            @Parameter(description = "ID assinado do evento", required = true) @PathVariable String idEvento,
            @Parameter(description = "ID assinado do claim de retirada", required = true) @PathVariable String idClaim,
            @Parameter(description = "Arquivos multipart (campo `anexos`)", required = true)
            @RequestParam("anexos") List<org.springframework.web.multipart.MultipartFile> anexos,
            HttpServletRequest http) {
        publicRateLimiter.check("portal-claim-comprovantes", ipDe(http));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(portalService.uploadComprovantesRetirada(idEvento, idClaim, anexos));
    }

    @PostMapping(value = "/eventos/{idEvento}/claims/{idClaim}/foto",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SecurityRequirements
    @Operation(summary = "Anexar foto ao relato de perda",
            description = "Endpoint público com rate limit por IP. "
                    + "Campo multipart `file`: JPEG/PNG até 5 MB. Somente claims do tipo PERDA.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Foto armazenada"),
            @ApiResponse(responseCode = "413", description = "Arquivo acima do limite"),
            @ApiResponse(responseCode = "415", description = "MIME não suportado"),
            @ApiResponse(responseCode = "429", description = "Rate limit excedido")
    })
    public ResponseEntity<ArquivoResponse> uploadFotoClaim(
            @Parameter(description = "ID assinado do evento", required = true) @PathVariable String idEvento,
            @Parameter(description = "ID assinado do claim", required = true) @PathVariable String idClaim,
            @Parameter(description = "Arquivo multipart (campo `file`)", required = true)
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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Claims do participante no evento"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Usuário sem ROLE_PARTICIPANTE")
    })
    public List<ClaimResponse> meusClaims(
            @Parameter(description = "ID assinado do evento", required = true) @PathVariable String idEvento,
            Authentication auth) {
        return portalService.meusClaims(idEvento, auth.getName());
    }

    @PostMapping("/eventos/{idEvento}/criancas")
    @SecurityRequirements
    @Operation(summary = "Cadastrar criança no portal (público)",
            description = "Endpoint público com rate limit por IP. Dados sujeitos a LGPD — use apenas o necessário.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Criança cadastrada"),
            @ApiResponse(responseCode = "429", description = "Rate limit excedido")
    })
    public ResponseEntity<CriancaResponse> cadastrarCrianca(
            @Parameter(description = "ID assinado do evento", required = true) @PathVariable String idEvento,
            @Valid @RequestBody CriancaCreateRequest request,
            HttpServletRequest http) {
        publicRateLimiter.check("portal-crianca", ipDe(http));
        return ResponseEntity.status(HttpStatus.CREATED).body(portalService.cadastrarCrianca(idEvento, request));
    }

    @PostMapping("/eventos/{idEvento}/criancas/responsaveis")
    @SecurityRequirements
    @Operation(summary = "Vincular responsável a uma criança (público)",
            description = "Endpoint público com rate limit por IP. O `idEvento` na URL identifica o contexto do portal.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Responsável vinculado"),
            @ApiResponse(responseCode = "429", description = "Rate limit excedido")
    })
    public ResponseEntity<CriancaResponsavelResponse> vincularResponsavel(
            @Parameter(description = "ID assinado do evento", required = true) @PathVariable String idEvento,
            @Valid @RequestBody CriancaResponsavelCreateRequest request,
            HttpServletRequest http) {
        publicRateLimiter.check("portal-responsavel", ipDe(http));
        return ResponseEntity.status(HttpStatus.CREATED).body(portalService.vincularResponsavel(request));
    }

    @PostMapping("/auth/registro")
    @SecurityRequirements
    @Operation(summary = "Registrar participante do portal",
            description = "Endpoint público com rate limit por IP. "
                    + "Cria usuário com perfil de participante para autenticar e consultar seus claims.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Participante registrado"),
            @ApiResponse(responseCode = "409", description = "E-mail já cadastrado"),
            @ApiResponse(responseCode = "429", description = "Rate limit excedido")
    })
    public ResponseEntity<UsuarioResponse> registrarParticipante(
            @Valid @RequestBody PortalParticipanteRegisterRequest request,
            HttpServletRequest http) {
        publicRateLimiter.check("portal-registro", ipDe(http));
        return ResponseEntity.status(HttpStatus.CREATED).body(portalService.registrarParticipante(request));
    }

    @GetMapping("/respostas/{token}")
    @SecurityRequirements
    @Operation(summary = "Contexto público do link de resposta",
            description = "Endpoint público com rate limit por IP. "
                    + "Valida o token do e-mail e retorna dados mínimos (sem PII sensível).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contexto válido"),
            @ApiResponse(responseCode = "410", description = "Token expirado ou já utilizado"),
            @ApiResponse(responseCode = "429", description = "Rate limit excedido")
    })
    public PortalRespostaContextResponse contextoResposta(
            @Parameter(description = "Token opaco enviado por e-mail", required = true) @PathVariable String token,
            HttpServletRequest http) {
        publicRateLimiter.check("portal-resposta-get", ipDe(http));
        return claimMensagemService.contextoPublico(token);
    }

    @PostMapping(value = "/respostas/{token}", consumes = {
            MediaType.MULTIPART_FORM_DATA_VALUE,
            MediaType.APPLICATION_FORM_URLENCODED_VALUE
    })
    @SecurityRequirements
    @Operation(summary = "Enviar resposta pública pelo link do e-mail",
            description = "Endpoint público com rate limit por IP. "
                    + "`dsMensagem` obrigatório; `imagens` opcionais (até 5 arquivos JPEG/PNG de 5 MB).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resposta registrada"),
            @ApiResponse(responseCode = "410", description = "Token expirado ou já utilizado"),
            @ApiResponse(responseCode = "413", description = "Arquivo acima do limite"),
            @ApiResponse(responseCode = "415", description = "MIME não suportado"),
            @ApiResponse(responseCode = "429", description = "Rate limit excedido")
    })
    public PortalRespostaSubmitResponse enviarResposta(
            @Parameter(description = "Token opaco enviado por e-mail", required = true) @PathVariable String token,
            @Parameter(description = "Texto da resposta", required = true) @RequestParam("dsMensagem") String dsMensagem,
            @Parameter(description = "Imagens opcionais (campo `imagens`)")
            @RequestParam(value = "imagens", required = false) List<org.springframework.web.multipart.MultipartFile> imagens,
            HttpServletRequest http) {
        publicRateLimiter.check("portal-resposta-post", ipDe(http));
        return claimMensagemService.responderPublico(token, dsMensagem, imagens);
    }
}
