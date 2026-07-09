package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoriaCampoCreateRequest(
        @NotBlank String idCategoria,
        @NotBlank String nmCampo,
        @NotBlank String dsLabel,
        @NotBlank String tpCampo,
        Integer qtTamanho,
        Boolean fgObrigatorio,
        Boolean fgPesquisavel,
        Integer orExibicao
) {}
