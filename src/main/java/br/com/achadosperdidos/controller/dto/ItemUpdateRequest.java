package br.com.achadosperdidos.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/** Atualização de item coletado. Campos nulos são ignorados (mantêm o valor atual). */
public record ItemUpdateRequest(
        String idCategoria,
        String idSubcategoria,
        String idStatus,
        String nmTitulo,
        String dsItem,
        String dsWallpaper,
        String dsObservacoes,
        String nmMarca,
        String nmModelo,
        String nmCor,
        String nmEstado,
        String dsTags,
        LocalDate dtEncontrado,
        LocalTime hrEncontrado,
        String nmLocalEncontrado,
        String nmPosto,
        String nmEncontradoPor,
        BigDecimal vlEstimado,
        String tpPrioridade,
        Boolean fgSensivel
) {}
