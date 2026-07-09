package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record ClaimValidacaoCreateRequest(
        @NotBlank String idClaim,
        @NotBlank String idItem,
        BigDecimal qtSimilaridade,
        String stResultado
) {}
