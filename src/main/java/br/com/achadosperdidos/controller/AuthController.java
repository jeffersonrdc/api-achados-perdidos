package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.*;
import br.com.achadosperdidos.exception.TooManyRequestsException;
import br.com.achadosperdidos.security.LoginRateLimiter;
import br.com.achadosperdidos.service.AuthEventService;
import br.com.achadosperdidos.service.AuthService;
import br.com.achadosperdidos.service.UsuarioService;
import br.com.achadosperdidos.util.ClientIpResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação", description = "Login, refresh e logout JWT. Endpoints públicos (sem Bearer).")
@SecurityRequirements
public class AuthController {
    private final AuthService authService;
    private final UsuarioService usuarioService;
    private final LoginRateLimiter loginRateLimiter;
    private final AuthEventService authEventService;
    private final ClientIpResolver clientIpResolver;

    public AuthController(AuthService authService, UsuarioService usuarioService, LoginRateLimiter loginRateLimiter,
                          AuthEventService authEventService, ClientIpResolver clientIpResolver) {
        this.authService = authService;
        this.usuarioService = usuarioService;
        this.loginRateLimiter = loginRateLimiter;
        this.authEventService = authEventService;
        this.clientIpResolver = clientIpResolver;
    }

    @PostMapping("/login")
    @Operation(
            summary = "Autenticar usuário",
            description = """
                    Autentica por e-mail ou login e retorna o par access/refresh JWT.

                    **OWASP A07:** rate limit por IP e por conta é aplicado **antes** da verificação de credenciais.
                    Credenciais inválidas retornam mensagem genérica (sem enumerar usuário).
                    Login bem-sucedido e falhas/bloqueios são registrados em `auth_event` (A09).
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticação bem-sucedida",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Payload inválido",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "429", description = "Rate limit (IP ou conta) — body inclui retryAfterSeconds",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        String ip = clientIpResolver.resolve(http);
        String navegador = truncar(http.getHeader("User-Agent"), 150);
        String dispositivo = detectarDispositivo(navegador);
        try {
            loginRateLimiter.checkIp(ip);
            loginRateLimiter.checkAccount(request.identificador());
        } catch (TooManyRequestsException ex) {
            String evento = TooManyRequestsException.MOTIVO_CONTA.equals(ex.getCodigoMotivo())
                    ? AuthEventService.LOGIN_RATE_LIMIT_CONTA
                    : AuthEventService.LOGIN_RATE_LIMIT_IP;
            authEventService.registrarIndependente(
                    evento, AuthEventService.RESULTADO_BLOQUEIO,
                    ex.getCodigoMotivo(), null, request.identificador(), ip, dispositivo, navegador);
            throw ex;
        }
        var tokens = authService.authenticate(request.identificador(), request.senha(), ip, dispositivo, navegador);
        loginRateLimiter.resetAccount(request.identificador());
        var usuario = usuarioService.findResumoByIdentificador(request.identificador());
        return ResponseEntity.ok(LoginResponse.of(tokens.accessToken(), tokens.refreshToken(), usuario));
    }

    private static String detectarDispositivo(String userAgent) {
        if (userAgent == null) return null;
        String ua = userAgent.toLowerCase();
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) return "Mobile";
        if (ua.contains("tablet") || ua.contains("ipad")) return "Tablet";
        return "Desktop";
    }

    private static String truncar(String v, int max) {
        if (v == null) return null;
        return v.length() > max ? v.substring(0, max) : v;
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Renovar tokens",
            description = """
                    Troca um refresh token válido por um novo par access/refresh (**rotação**).
                    O refresh anterior é revogado (A04/A07). Tokens com `typ` diferente de `refresh` são rejeitados.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tokens renovados",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RefreshResponse.class))),
            @ApiResponse(responseCode = "401", description = "Refresh inválido, expirado ou revogado",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest request, HttpServletRequest http) {
        String ip = clientIpResolver.resolve(http);
        String navegador = truncar(http.getHeader("User-Agent"), 150);
        var tokens = authService.refresh(request.refreshToken(), ip, detectarDispositivo(navegador), navegador);
        return ResponseEntity.ok(RefreshResponse.of(tokens.accessToken(), tokens.refreshToken()));
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Encerrar sessão (revogar refresh)",
            description = "Revoga o refresh token informado. Idempotente para tokens já inválidos/revogados."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Refresh revogado (ou já inválido)"),
            @ApiResponse(responseCode = "400", description = "Payload inválido",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request, HttpServletRequest http) {
        String ip = clientIpResolver.resolve(http);
        String navegador = truncar(http.getHeader("User-Agent"), 150);
        authService.logout(request.refreshToken(), ip, detectarDispositivo(navegador), navegador);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
