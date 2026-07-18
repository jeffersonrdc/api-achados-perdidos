package br.com.achadosperdidos.controller.dto;

public record EstoqueEnderecoResponse(
        String id,
        String idDeposito,
        String tpNivel,
        String nmEndereco,
        Integer orOrdem,
        Boolean fgAtivo,
        String idEnderecoPai,
        String nmEnderecoPai
) {}
