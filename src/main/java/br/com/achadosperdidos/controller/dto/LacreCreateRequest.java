package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record LacreCreateRequest(
        @NotBlank String nrLacre,
        String nrCodigoBarra,
        String nrQrCode,
        Boolean fgViolado,
        String dsObservacao
) {}
