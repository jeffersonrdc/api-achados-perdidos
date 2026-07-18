package br.com.achadosperdidos.controller.dto;

public record TagUpdateRequest(
        String nmTag,
        String idSubcategoria,
        String dsTag,
        Integer orOrdem,
        Boolean fgAtivo
) {}
