package br.com.achadosperdidos.controller.dto;

import java.util.List;

/** KPIs/cards da tela de Transferências. Totais do evento. */
public record TransferenciaResumoResponse(
        long total,
        long concluidas,
        long itensTransferidos,
        long locaisDestino,
        List<DestinoQt> porDestino
) {
    public record DestinoQt(String nome, long qt) {}
}
