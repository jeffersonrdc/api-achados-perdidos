package br.com.achadosperdidos.controller.dto;

public record DashboardEventoResponse(
        String idEvento, String nmEvento, Long qtItensTotal, Long qtItensPendentes,
        Long qtItensDevolvidos, Long qtClaimsTotal
) {}
