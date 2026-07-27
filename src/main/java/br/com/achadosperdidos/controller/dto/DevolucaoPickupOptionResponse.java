package br.com.achadosperdidos.controller.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record DevolucaoPickupOptionResponse(
        String id,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String pickupLocationName,
        LocalDateTime expiresAt,
        String notes,
        Boolean selected
) {}
