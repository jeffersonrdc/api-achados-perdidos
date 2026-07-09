package br.com.achadosperdidos.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ItemCampoResponse(
        String id,
        String idItem,
        String idCategoriaCampo,
        String nmCampo,
        String vlTexto,
        BigDecimal vlNumero,
        LocalDate vlData,
        Boolean vlBoolean
) {}
