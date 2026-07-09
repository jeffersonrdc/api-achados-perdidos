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
        LocalDateTime dtAuditoria,
        String nrIp
) {}
