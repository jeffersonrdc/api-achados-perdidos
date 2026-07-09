package br.com.achadosperdidos.controller.dto;

import java.time.LocalDateTime;

public record ItemHistoricoResponse(
        String id,
        String idItem,
        String idStatusAnterior,
        String idStatusNovo,
        String nmStatusAnterior,
        String nmStatusNovo,
        String dsHistorico,
        LocalDateTime dtHistorico
) {}
