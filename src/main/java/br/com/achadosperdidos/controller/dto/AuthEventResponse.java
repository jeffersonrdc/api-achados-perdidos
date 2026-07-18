package br.com.achadosperdidos.controller.dto;

import java.time.LocalDateTime;

public record AuthEventResponse(
        String id,
        String tpEvento,
        String tpResultado,
        String cdMotivo,
        String idUsuario,
        String nmUsuario,
        String dsIdentificadorMascarado,
        String nrIp,
        String nmDispositivo,
        String nmNavegador,
        LocalDateTime dtEvento
) {}
