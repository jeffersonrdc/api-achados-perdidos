package br.com.achadosperdidos.controller.dto;

public record EstoqueEnderecoUpdateRequest(
        String nmEndereco,
        String idEnderecoPai,
        Integer orOrdem,
        Boolean fgAtivo
) {}
