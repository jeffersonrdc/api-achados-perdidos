package br.com.achadosperdidos.controller.dto;

public record EtiquetaImprimirRequest(
        String tpImpressao,
        String nmImpressora,
        String nrIdentificador,
        String dsMotivo
) {}
