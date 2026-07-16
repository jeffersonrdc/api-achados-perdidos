package br.com.achadosperdidos.controller.dto;

import java.util.List;

/** KPIs/cards da tela de Estoque (/estoque). Totais de todo o estoque, não da página. */
public record EstoqueResumoResponse(
        long total,
        long comLocalizacao,
        long semLocalizacao,
        long depositos,
        long sensiveis,
        List<DepositoQt> porDeposito
) {
    public record DepositoQt(String nome, long qt) {}
}
