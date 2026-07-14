package br.com.achadosperdidos.controller.dto;

/** KPIs/cards da tela de Coleta de Itens (/itens). */
public record ColetaResumoResponse(
        long total,
        long coletados,
        long solicitacoesPendentes,
        long aguardandoTriagem,
        long emTriagem,
        long sensiveis,
        long altaPrioridade
) {}
