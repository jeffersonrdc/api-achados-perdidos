package br.com.achadosperdidos.controller.dto;

/** Cards/resumo da tela de pedidos de devolução (/pedidos). */
public record ClaimResumoResponse(
        long total,
        long abertos,
        long emAnalise,
        long aprovados,
        long rejeitados
) {}
