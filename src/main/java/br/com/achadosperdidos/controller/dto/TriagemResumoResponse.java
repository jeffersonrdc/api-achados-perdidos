package br.com.achadosperdidos.controller.dto;

import java.util.List;

/** KPIs/cards da tela de Triagem (/triagem). Totais de toda a fila, não da página. */
public record TriagemResumoResponse(
        long total,
        long aguardando,
        long emAnalise,
        long emTriagem,
        long sensiveis,
        long categorias,
        List<CategoriaQt> porCategoria
) {
    public record CategoriaQt(String nome, long qt) {}
}
