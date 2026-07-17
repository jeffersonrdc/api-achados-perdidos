package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;

/** Solicitação de mais informações ao solicitante (pergunta em texto ou pedido de imagem). */
public record ClaimSolicitarInfoRequest(
        @NotBlank String tpSolicitacao,  // PERGUNTA | IMAGEM
        @NotBlank String dsDetalhe
) {}
