package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record MarcaCreateRequest(@NotBlank String nmMarca, Integer orOrdem, Boolean fgAtivo) {}
