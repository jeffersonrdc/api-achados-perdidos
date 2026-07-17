package br.com.achadosperdidos.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "LoginRequest", description = "Credenciais de autenticação (e-mail ou login)")
public record LoginRequest(
        @NotBlank
        @Schema(description = "E-mail (NM_Email) ou login (NM_Login) do usuário",
                example = "operador@evento.com",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String identificador,
        @NotBlank
        @Schema(description = "Senha em texto plano (transmitida apenas via HTTPS em produção)",
                example = "********",
                format = "password",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String senha
) {}
