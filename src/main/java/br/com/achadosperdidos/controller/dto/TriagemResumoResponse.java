package br.com.achadosperdidos.controller.dto;

/** KPIs/cards da tela de Triagem (/triagem). */
public record TriagemResumoResponse(
        long total,
        long aguardando,
        long emAnalise,
        long emTriagem,
        long sensiveis,
        long categorias
) {}
