package br.com.achadosperdidos.controller.dto;

import java.time.LocalDate;

/** Ponto da série temporal de evolução operacional do evento (Dashboard). */
public record EvolucaoPontoResponse(
        LocalDate data,
        long encontrados,
        long devolvidos,
        long solicitacoes) {
}
