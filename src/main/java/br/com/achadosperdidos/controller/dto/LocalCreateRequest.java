package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record LocalCreateRequest(
        @NotBlank String idEvento,
        @NotBlank String nmLocal,
        @NotBlank String tpLocal,
        String idResponsavel,
        BigDecimal vlLatitude,
        BigDecimal vlLongitude,
        String nmHorario,
        String dsObservacao,
        Boolean fgAtivo
) {}
