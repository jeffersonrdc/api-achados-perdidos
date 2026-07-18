package br.com.achadosperdidos.controller.dto;

public record TagResponse(
        String id,
        String nmTag,
        String dsTag,
        Integer orOrdem,
        Boolean fgAtivo,
        String idSubcategoria,
        String nmSubcategoria,
        String idCategoria,
        String nmCategoria
) {}
