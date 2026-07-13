package br.com.achadosperdidos.controller.dto;

import java.time.LocalDateTime;

/** Atualização de evento. Campos nulos preservam o valor atual (exceto fgAtivo). */
public record EventoUpdateRequest(
        String nmEvento,
        String dsEvento,
        LocalDateTime dtInicio,
        LocalDateTime dtFim,
        String nmLocal,
        String nmCidade,
        String sgUf,
        Integer qtDiasRetencao,
        Boolean fgAtivo
) {}
