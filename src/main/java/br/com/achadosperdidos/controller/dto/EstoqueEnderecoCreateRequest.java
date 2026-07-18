package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record EstoqueEnderecoCreateRequest(
        @NotBlank String tpNivel,
        @NotBlank String nmEndereco,
        String idEnderecoPai,
        Integer orOrdem,
        Boolean fgAtivo
) {}
