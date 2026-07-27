package br.com.achadosperdidos.controller.dto;

public record DevolucaoShippingAddressResponse(
        String recipientName,
        String zipCode,
        String street,
        String number,
        String complement,
        String district,
        String city,
        String state,
        String phone
) {}
