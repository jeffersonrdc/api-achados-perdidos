package br.com.achadosperdidos.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta de autenticação")
public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tipoToken,
        UsuarioResumoResponse usuario
) {
    public static LoginResponse of(String access, String refresh, UsuarioResumoResponse usuario) {
        return new LoginResponse(access, refresh, "Bearer", usuario);
    }
}
