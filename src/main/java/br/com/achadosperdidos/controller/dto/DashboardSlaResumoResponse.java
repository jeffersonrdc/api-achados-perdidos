package br.com.achadosperdidos.controller.dto;

public record DashboardSlaResumoResponse(
        String idEvento,
        String nmEvento,
        String tpProcesso,
        long qtTotal,
        long qtEmAndamento,
        long qtAlerta,
        long qtEstourado,
        long qtConcluido) {}
