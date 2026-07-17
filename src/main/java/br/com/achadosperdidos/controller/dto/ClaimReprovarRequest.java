package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;

/** Reprovação do pedido. idItem é opcional (quando a reprovação é referente a um item específico). */
public record ClaimReprovarRequest(
        String idItem,
        @NotBlank String dsMotivo
) {}
