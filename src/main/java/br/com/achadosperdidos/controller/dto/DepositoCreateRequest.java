package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record DepositoCreateRequest(@NotBlank String idEvento, @NotBlank String nmDeposito, String dsDeposito, Boolean fgPrincipal) {}
