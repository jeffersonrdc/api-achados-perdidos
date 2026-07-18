package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record TagCreateRequest(
        @NotBlank String nmTag,
        @NotBlank String idSubcategoria,
        String dsTag,
        Integer orOrdem,
        Boolean fgAtivo
) {}
