package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record ModeloCreateRequest(
        @NotBlank String nmModelo,
        @NotBlank String idMarca,
        Integer orOrdem,
        Boolean fgAtivo
) {}
