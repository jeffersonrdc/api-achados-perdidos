package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;

/** Aprovação do pedido: vincula o claim ao item físico e gera a devolução. */
public record ClaimAprovarRequest(
        @NotBlank String idItem,
        String dsObservacao
) {}
