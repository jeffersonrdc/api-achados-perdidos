package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;

/** Pedido de retirada (RETIRADA) vinculado a um item do estoque — tela /pedidos. */
public record ClaimCreateItemRequest(
        @NotBlank String idEvento,
        @NotBlank String idItem,
        @NotBlank String nmNome,
        @NotBlank String nmEmail,
        String nrTelefone,
        String nmContatoConfianca,
        String nrTelefoneConfianca,
        String dsRelacaoContatoConfianca,
        String dsObjeto,
        String dsDetalhesOcultos,
        String nmOperador
) {}
