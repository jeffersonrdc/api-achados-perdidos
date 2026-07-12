package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;

/** Solicita a transicao de um item para o status de destino informado (por nome). */
public record ItemTransicaoRequest(
        @NotBlank String nmStatusDestino,
        String dsObservacao
) {}
