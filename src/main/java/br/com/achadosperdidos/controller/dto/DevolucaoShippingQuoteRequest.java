package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DevolucaoShippingQuoteRequest(
        @NotNull BigDecimal amount,
        String currency,
        @NotNull Integer estimatedDeliveryDays,
        @NotNull Integer postingDeadlineDaysAfterPayment,
        @NotBlank String paymentInstructions,
        Boolean sendEmail
) {}
