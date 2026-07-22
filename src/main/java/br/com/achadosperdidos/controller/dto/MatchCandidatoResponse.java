package br.com.achadosperdidos.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/** Candidato da coleta sugerido como correspondência de um claim PERDA. */
public record MatchCandidatoResponse(
        String id,
        String cdItem,
        String nmTitulo,
        String nmCategoria,
        String nmSubcategoria,
        String nmMarca,
        String nmModelo,
        String nmCor,
        String nmEstado,
        String nmLocalEncontrado,
        LocalDate dtEncontrado,
        LocalTime hrEncontrado,
        String tpPrioridade,
        String nmStatus,
        BigDecimal vlScore
) {}
