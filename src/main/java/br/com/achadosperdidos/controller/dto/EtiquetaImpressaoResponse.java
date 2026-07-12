package br.com.achadosperdidos.controller.dto;

import java.time.LocalDateTime;

public record EtiquetaImpressaoResponse(
        String id,
        String idItem,
        String tpImpressao,
        String nmImpressora,
        String nrIdentificador,
        String dsMotivo,
        String idOperador,
        String nmOperador,
        LocalDateTime dtImpressao
) {}
