package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record PortalClaimItemRequest(
        @NotBlank String idItem,
        @NotBlank String nmNome,
        String nrCpf,
        @NotBlank String nmEmail,
        String nrTelefone,
        String dsObservacao
) {}
