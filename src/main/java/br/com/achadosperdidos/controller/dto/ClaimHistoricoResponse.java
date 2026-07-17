package br.com.achadosperdidos.controller.dto;

import java.time.LocalDateTime;

public record ClaimHistoricoResponse(
        String id,
        String tpEvento,
        String tpSolicitacao,
        String dsDetalhe,
        String cdItem,
        String nmOperador,
        Boolean fgEmailEnviado,
        String dsEmailErro,
        LocalDateTime dtHistorico
) {}
