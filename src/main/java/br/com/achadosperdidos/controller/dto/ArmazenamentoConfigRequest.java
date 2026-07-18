package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record ArmazenamentoConfigRequest(
        @NotBlank String provider
) {}
