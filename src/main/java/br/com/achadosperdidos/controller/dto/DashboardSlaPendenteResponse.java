package br.com.achadosperdidos.controller.dto;

import java.time.LocalDateTime;

public record DashboardSlaPendenteResponse(
        String idSlaRegistro,
        String tpEntidade,
        String idEntidade,
        LocalDateTime dtInicio,
        LocalDateTime dtLimite,
        String stSla,
        String tpProcesso,
        Integer qtHorasLimite,
        Integer qtHorasAlerta,
        Long qtHorasRestantes,
        String nmEvento
) {}
