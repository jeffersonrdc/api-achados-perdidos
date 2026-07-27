package br.com.achadosperdidos.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DevolucaoShippingQuoteResponse(
        BigDecimal amount,
        String currency,
        Integer estimatedDeliveryDays,
        Integer postingDeadlineDaysAfterPayment,
        String paymentInstructions,
        LocalDateTime informedAt
) {}
