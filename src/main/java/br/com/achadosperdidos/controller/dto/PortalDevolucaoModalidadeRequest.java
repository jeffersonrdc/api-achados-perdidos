package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record PortalDevolucaoModalidadeRequest(@NotBlank String method) {}
