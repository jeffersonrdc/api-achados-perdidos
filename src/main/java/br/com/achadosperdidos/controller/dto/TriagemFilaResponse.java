package br.com.achadosperdidos.controller.dto;

import java.time.LocalDate;

public record TriagemFilaResponse(
        String idItem,
        String cdItem,
        String nmTitulo,
        String nmCategoria,
        String nmStatus,
        String tpPrioridade,
        Boolean fgSensivel,
        LocalDate dtEncontrado
) {}
