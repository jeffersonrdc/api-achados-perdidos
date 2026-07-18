package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Aprovação do pedido: vincula o claim ao item físico, exige justificativa e gera a devolução. */
public record ClaimAprovarRequest(
        @NotBlank String idItem,
        @NotBlank @Size(max = 1000) String dsJustificativa
) {}
