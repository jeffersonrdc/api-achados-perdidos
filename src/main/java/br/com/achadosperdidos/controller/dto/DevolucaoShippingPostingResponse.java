package br.com.achadosperdidos.controller.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DevolucaoShippingPostingResponse(
        LocalDate postingDate,
        String trackingCode,
        LocalDateTime registeredAt
) {}
