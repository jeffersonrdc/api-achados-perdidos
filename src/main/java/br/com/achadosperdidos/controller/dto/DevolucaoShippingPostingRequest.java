package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record DevolucaoShippingPostingRequest(
        @NotNull LocalDate postingDate,
        @NotBlank String trackingCode,
        Boolean sendEmail,
        String notes
) {}
