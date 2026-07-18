package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record CorCreateRequest(@NotBlank String nmCor, String cdHex, Integer orOrdem, Boolean fgAtivo) {}
