package br.com.achadosperdidos.controller.dto;

import java.time.LocalDateTime;

public record AuditoriaResponse(
        String id,
        String nmTabela,
        String idRegistro,
        String tpAcao,
        String dsAntes,
        String dsDepois,
        String idUsuario,
        String nmUsuario,
        LocalDateTime dtAuditoria,
        String nrIp
) {}
