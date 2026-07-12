package br.com.achadosperdidos.controller.dto;

import java.math.BigDecimal;

public record LocalUpdateRequest(
        String nmLocal,
        String tpLocal,
        String idResponsavel,
        BigDecimal vlLatitude,
        BigDecimal vlLongitude,
        String nmHorario,
        String dsObservacao,
        Boolean fgAtivo
) {}
