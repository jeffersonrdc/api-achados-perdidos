package br.com.achadosperdidos.controller.dto;

public record CategoriaCampoResponse(
        String id,
        String idCategoria,
        String nmCampo,
        String dsLabel,
        String tpCampo,
        Integer qtTamanho,
        Boolean fgObrigatorio,
        Boolean fgPesquisavel,
        Integer orExibicao
) {}
