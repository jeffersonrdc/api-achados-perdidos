package br.com.achadosperdidos.controller.dto;

import java.math.BigDecimal;

public record LocalResponse(
        String id,
        String idEvento,
        String nmLocal,
        String tpLocal,
        String idResponsavel,
        String nmResponsavel,
        BigDecimal vlLatitude,
        BigDecimal vlLongitude,
        String nmHorario,
        String dsObservacao,
        Boolean fgAtivo
) {}
