package br.com.achadosperdidos.controller.dto;

import java.time.LocalDateTime;

public record LacreResponse(
        String id,
        String nrLacre,
        String nrCodigoBarra,
        String nrQrCode,
        Boolean fgViolado,
        String dsObservacao,
        LocalDateTime dtLacre
) {}
