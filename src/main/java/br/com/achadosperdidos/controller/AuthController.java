package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.*;
import br.com.achadosperdidos.service.AuthService;
import br.com.achadosperdidos.service.UsuarioService;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        var tokens = authService.authenticate(request.identificador(), request.senha());
        var usuario = usuarioService.findResumoByIdentificador(request.identificador());
        return ResponseEntity.ok(LoginResponse.of(tokens.accessToken(), tokens.refreshToken(), usuario));
    }
    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        var tokens = authService.refresh(request.refreshToken());
        return ResponseEntity.ok(RefreshResponse.of(tokens.accessToken(), tokens.refreshToken()));
    }
}
