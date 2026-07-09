package br.com.achadosperdidos.controller.dto;

public record SlaRegraResponse(
        String id,
        String idEvento,
        String tpProcesso,
        Integer qtHorasLimite,
        Integer qtHorasAlerta,
        Boolean fgEnviarAlerta,
        String dsObservacao
) {}
