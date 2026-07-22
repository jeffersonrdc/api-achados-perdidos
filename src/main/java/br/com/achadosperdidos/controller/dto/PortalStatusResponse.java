package br.com.achadosperdidos.controller.dto;

import java.time.LocalDateTime;

/** Status de liberação do portal público (baseado em dtInicio/dtFim do evento). */
public record PortalStatusResponse(
        boolean liberado,
        String idEvento,
        String nmEvento,
        LocalDateTime dtInicio,
        LocalDateTime dtFim,
        String mensagem
) {}
