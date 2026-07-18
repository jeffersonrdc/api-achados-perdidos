package br.com.achadosperdidos.controller.dto;

public record EstadoUpdateRequest(
        String nmEstado,
        String dsEstado,
        Integer orOrdem,
        Boolean fgAtivo
) {}
