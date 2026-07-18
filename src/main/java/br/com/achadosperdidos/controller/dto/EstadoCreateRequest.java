package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record EstadoCreateRequest(
        @NotBlank String nmEstado,
        String dsEstado,
        Integer orOrdem,
        Boolean fgAtivo
) {}
