package br.com.achadosperdidos.controller.dto;

/** Item disponível para transferência (está em um local). */
public record ItemDisponivelResponse(
        String idItem,
        String cdItem,
        String nmTitulo,
        String nmCategoria,
        String tpPrioridade,
        Boolean fgSensivel,
        String nmLocalAtual
) {}
