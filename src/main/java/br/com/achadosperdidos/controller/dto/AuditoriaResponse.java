package br.com.achadosperdidos.controller.dto;

import java.time.LocalDateTime;

public record AuditoriaResponse(
        String id,
        String nmTabela,
        String nmModulo,
        String idRegistro,
        String tpAcao,
        String dsAntes,
        String dsDepois,
        String idUsuario,
        String nmUsuario,
        String nmLogin,
        LocalDateTime dtAuditoria,
        /** Data/hora de criação do registro (quando presente no snapshot JSON). */
        LocalDateTime dtRegistroCriado,
        /** Data/hora da última alteração do registro (quando presente no snapshot JSON). */
        LocalDateTime dtRegistroAtualizado,
        Integer qtCamposAlterados,
        String dsResumo,
        String nrIp
) {}
