package br.com.achadosperdidos.controller.dto;

public record ModeloResponse(
        String id,
        String nmModelo,
        Integer orOrdem,
        Boolean fgAtivo,
        String idMarca,
        String nmMarca
) {}
