package br.com.achadosperdidos.controller.dto;

public record RefreshResponse(String accessToken, String refreshToken, String tipoToken) {
    public static RefreshResponse of(String access, String refresh) {
        return new RefreshResponse(access, refresh, "Bearer");
    }
}
