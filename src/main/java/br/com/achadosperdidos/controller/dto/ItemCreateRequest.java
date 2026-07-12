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
        String nmMarca,
        String nmModelo,
        String nmCor,
        @NotNull LocalDate dtEncontrado,
        LocalTime hrEncontrado,
        String nmLocalEncontrado,
        BigDecimal vlEstimado,
        String tpPrioridade,
        Boolean fgSensivel
) {}
