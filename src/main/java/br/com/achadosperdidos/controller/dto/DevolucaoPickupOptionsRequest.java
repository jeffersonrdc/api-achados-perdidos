package br.com.achadosperdidos.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record DevolucaoPickupOptionsRequest(
        @NotEmpty @Valid List<PickupOptionItem> options,
        Boolean sendEmail
) {
    public record PickupOptionItem(
            @NotNull LocalDate date,
            @NotNull LocalTime startTime,
            @NotNull LocalTime endTime,
            String pickupLocationId,
            String pickupLocationName,
            LocalDateTime expiresAt,
            String notes
    ) {}
}
