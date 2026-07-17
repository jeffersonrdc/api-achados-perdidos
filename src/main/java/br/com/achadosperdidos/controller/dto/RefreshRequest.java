package br.com.achadosperdidos.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "RefreshRequest", description = "Pedido de renovação do par access/refresh")
public record RefreshRequest(
        @NotBlank
        @Schema(description = "Refresh token vigente (será rotacionado/revogado)",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String refreshToken
) {}
