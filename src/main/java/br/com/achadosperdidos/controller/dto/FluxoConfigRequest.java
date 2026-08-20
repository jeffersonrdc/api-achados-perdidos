package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotNull;

public record FluxoConfigRequest(
        @NotNull Boolean triagemObrigatoria
) {}
