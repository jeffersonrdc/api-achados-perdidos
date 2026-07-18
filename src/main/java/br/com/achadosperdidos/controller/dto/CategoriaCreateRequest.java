package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoriaCreateRequest(
        @NotBlank String nmCategoria,
        String idCategoriaPai,
        String dsCategoria,
        String icIcone,
        Integer orOrdem,
        Boolean fgAtivo
) {}
