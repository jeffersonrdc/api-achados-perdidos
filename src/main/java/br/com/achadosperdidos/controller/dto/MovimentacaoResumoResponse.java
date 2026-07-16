package br.com.achadosperdidos.controller.dto;

import java.util.List;

/** KPIs/cards da tela de Transferências (/transferencias). Totais de todo o evento. */
public record MovimentacaoResumoResponse(
        long total,
        long transferencias,
        long armazenamentos,
        long outros,
        List<TipoQt> porTipo
) {
    public record TipoQt(String nome, long qt) {}
}
