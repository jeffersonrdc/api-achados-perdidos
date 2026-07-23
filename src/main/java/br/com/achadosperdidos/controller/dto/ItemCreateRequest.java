package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record ItemCreateRequest(
        @NotBlank String idEvento,
        @NotBlank String idCategoria,
        String idSubcategoria,
        String idStatus,
        @NotBlank String nmTitulo,
        String dsItem,
        String dsObservacoes,
        String nmMarca,
        String nmModelo,
        String nmCor,
        String nmEstado,
        String dsTags,
        @NotNull LocalDate dtEncontrado,
        LocalTime hrEncontrado,
        String nmLocalEncontrado,
        String nmPosto,
        String nmEncontradoPor,
        BigDecimal vlEstimado,
        String tpPrioridade,
        Boolean fgSensivel
) {}
