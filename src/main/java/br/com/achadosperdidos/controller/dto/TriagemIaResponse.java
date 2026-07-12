package br.com.achadosperdidos.controller.dto;

import java.math.BigDecimal;

/** Sugestao automatica (stub) para apoiar a classificacao na triagem. */
public record TriagemIaResponse(
        String dsSugestao,
        BigDecimal vlConfianca
) {}
