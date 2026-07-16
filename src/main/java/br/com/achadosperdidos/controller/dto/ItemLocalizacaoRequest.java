package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;

/** Atualização da localização física do item no estoque. */
public record ItemLocalizacaoRequest(
        @NotBlank String idDeposito,
        String nmSetor,
        String nmCorredor,
        String nmEstante,
        String nmPrateleira,
        String nmCaixa,
        String nmPosicao,
        String dsMotivo
) {}
