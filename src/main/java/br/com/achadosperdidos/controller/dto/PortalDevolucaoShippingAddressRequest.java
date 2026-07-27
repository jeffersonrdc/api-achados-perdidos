package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record PortalDevolucaoShippingAddressRequest(
        @NotBlank String recipientName,
        @NotBlank String zipCode,
        @NotBlank String street,
        @NotBlank String number,
        String complement,
        @NotBlank String district,
        @NotBlank String city,
        @NotBlank String state,
        @NotBlank String phone
) {}
