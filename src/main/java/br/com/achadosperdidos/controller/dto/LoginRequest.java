package br.com.achadosperdidos.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciais de login (email ou login)")
public record LoginRequest(
        @NotBlank @Schema(description = "NM_Email ou NM_Login") String identificador,
        @NotBlank @Schema(description = "Senha") String senha
) {}
