package br.com.achadosperdidos.controller.dto;

public record EstadoResponse(
        String id,
        String nmEstado,
        String dsEstado,
        Integer orOrdem,
        Boolean fgAtivo
) {}
