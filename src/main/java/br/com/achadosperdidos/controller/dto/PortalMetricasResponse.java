package br.com.achadosperdidos.controller.dto;

/** KPIs públicos exibidos em /como-funciona (agregado dos eventos do portal). */
public record PortalMetricasResponse(
        long qtItensRegistrados,
        long qtItensDevolvidos,
        int pcTaxaSucesso,
        int hrTempoMedioResolucao
) {}
