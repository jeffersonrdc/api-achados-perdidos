package br.com.achadosperdidos.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ClaimValidacaoResponse(
        String id,
        String idClaim,
        String idItem,
        BigDecimal qtSimilaridade,
        String stResultado,
        LocalDateTime dtValidacao
) {}
