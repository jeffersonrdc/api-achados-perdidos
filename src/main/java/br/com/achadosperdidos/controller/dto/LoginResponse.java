package br.com.achadosperdidos.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LoginResponse", description = "Par de tokens JWT e resumo do usuário autenticado")
public record LoginResponse(
        @Schema(description = "Access token JWT (claim typ=access). Usar no header Authorization.",
                example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,
        @Schema(description = "Refresh token JWT (claim typ=refresh + jti). Usar em /auth/refresh ou /auth/logout.",
                example = "eyJhbGciOiJIUzI1NiJ9...")
        String refreshToken,
        @Schema(description = "Tipo do token de autorização", example = "Bearer", allowableValues = {"Bearer"})
        String tipoToken,
        @Schema(description = "Dados resumidos do usuário autenticado")
        UsuarioResumoResponse usuario
) {
    public static LoginResponse of(String access, String refresh, UsuarioResumoResponse usuario) {
        return new LoginResponse(access, refresh, "Bearer", usuario);
    }
}
