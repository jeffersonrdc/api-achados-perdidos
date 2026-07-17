package br.com.achadosperdidos.controller.dto;

/** Métricas de pedidos/reprovações para um item (exibidas ao aprovar). */
public record ItemClaimsResumoResponse(
        long pedidos,
        long reprovacoes,
        boolean aprovado
) {}
