package br.com.achadosperdidos.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "RefreshResponse", description = "Novo par de tokens após rotação do refresh")
public record RefreshResponse(
        @Schema(description = "Novo access token JWT", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,
        @Schema(description = "Novo refresh token (o anterior foi revogado)", example = "eyJhbGciOiJIUzI1NiJ9...")
        String refreshToken,
        @Schema(description = "Tipo do token", example = "Bearer", allowableValues = {"Bearer"})
        String tipoToken
) {
    public static RefreshResponse of(String access, String refresh) {
        return new RefreshResponse(access, refresh, "Bearer");
    }
}
