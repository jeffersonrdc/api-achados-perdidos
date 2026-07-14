package br.com.achadosperdidos.controller.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record TriagemFilaResponse(
        String idItem,
        String cdItem,
        String nmTitulo,
        String nmCategoria,
        String nmSubcategoria,
        String nmCor,
        String nmMarca,
        String nmModelo,
        String nmEstado,
        String nmStatus,
        String tpPrioridade,
        Boolean fgSensivel,
        LocalDate dtEncontrado,
        LocalTime hrEncontrado,
        String nmLocalEncontrado,
        String nmRecebidoPor
) {}
