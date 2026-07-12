package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.*;
import br.com.achadosperdidos.service.AuthService;
import br.com.achadosperdidos.service.UsuarioService;
import br.com.achadosperdidos.util.IpAddressUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação")
public class AuthController {
    private final AuthService authService;
    private final UsuarioService usuarioService;
    public AuthController(AuthService authService, UsuarioService usuarioService) {
        this.authService = authService; this.usuarioService = usuarioService;
    }
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        String ip = IpAddressUtil.normalize(resolverIp(http));
        String navegador = truncar(http.getHeader("User-Agent"), 150);
        var tokens = authService.authenticate(request.identificador(), request.senha(), ip, detectarDispositivo(navegador), navegador);
        var usuario = usuarioService.findResumoByIdentificador(request.identificador());
        return ResponseEntity.ok(LoginResponse.of(tokens.accessToken(), tokens.refreshToken(), usuario));
    }

    private static String resolverIp(HttpServletRequest http) {
        String fwd = http.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) return fwd.split(",")[0].trim();
        return http.getRemoteAddr();
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
    public ResponseEntity<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        var tokens = authService.refresh(request.refreshToken());
        return ResponseEntity.ok(RefreshResponse.of(tokens.accessToken(), tokens.refreshToken()));
    }
}
