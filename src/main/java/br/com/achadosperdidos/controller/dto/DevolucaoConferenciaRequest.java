package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotNull;

public record DevolucaoConferenciaRequest(
        @NotNull Boolean itemConferido,
        @NotNull Boolean documentoConferido
) {}
