package br.com.achadosperdidos.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record PortalDevolucaoContextResponse(
        String actionType,
        String protocolo,
        String objeto,
        String evento,
        String status,
        String method,
        LocalDateTime expiresAt,
        boolean expired,
        boolean used,
        String nextActionHint,
        List<PickupOption> pickupOptions,
        ShippingQuote shippingQuote,
        Tracking tracking
) {
    public record PickupOption(
            String id,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            String pickupLocationName,
            String notes
    ) {}

    public record ShippingQuote(
            BigDecimal amount,
            String currency,
            Integer estimatedDeliveryDays,
            Integer postingDeadlineDaysAfterPayment,
            String paymentInstructions
    ) {}

    public record Tracking(LocalDate postingDate, String trackingCode) {}
}
