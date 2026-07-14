package br.com.achadosperdidos.controller.dto;

public record CategoriaUpdateRequest(
        String nmCategoria,
        String dsCategoria,
        String icIcone,
        Integer orOrdem,
        Boolean fgAtivo
) {}
