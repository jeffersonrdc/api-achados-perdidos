package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ItemCampoUpsertRequest(
        @NotBlank String idItem,
        @NotBlank String idCategoriaCampo,
        String vlTexto,
        BigDecimal vlNumero,
        LocalDate vlData,
        Boolean vlBoolean
) {}
