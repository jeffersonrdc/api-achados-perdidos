package br.com.achadosperdidos.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Abertura do pedido de devolução a partir de um match. */
public record ClaimRetornoRequest(
        @Schema(description = "ID assinado do item escolhido no match. Sem valor, resolve pelo match confirmado.",
                example = "s2.abc123")
        String idItem
) {}
