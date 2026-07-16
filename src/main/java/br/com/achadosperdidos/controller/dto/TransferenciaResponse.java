package br.com.achadosperdidos.controller.dto;

import java.time.LocalDateTime;

public record TransferenciaResponse(
        String id,
        String idItem,
        String cdItem,
        String nmTitulo,
        String nmCategoria,
        Boolean fgSensivel,
        String origem,
        String destino,
        String nmResponsavel,
        String nmReceptor,
        String dsMotivo,
        String tpStatus,
        LocalDateTime dtTransferencia
) {}
