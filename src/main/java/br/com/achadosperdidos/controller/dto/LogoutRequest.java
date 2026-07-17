package br.com.achadosperdidos.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "LogoutRequest", description = "Revogação de sessão via refresh token")
public record LogoutRequest(
        @NotBlank
        @Schema(description = "Refresh token a revogar",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String refreshToken
) {}
