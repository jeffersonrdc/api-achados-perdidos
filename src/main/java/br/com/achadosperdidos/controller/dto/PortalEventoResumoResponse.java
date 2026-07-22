package br.com.achadosperdidos.controller.dto;

import java.time.LocalDateTime;

public record PortalEventoResumoResponse(
        String id,
        String nmEvento,
        String nmLocal,
        String nmCidade,
        String sgUf,
        LocalDateTime dtInicio,
        LocalDateTime dtFim,
        Boolean fgConsultaPublica,
        Boolean fgAceitaClaim,
        /** ID assinado — GET /portal/arquivos/{id}/download ou /thumbnail */
        String idLogo,
        /** ID assinado — GET /portal/arquivos/{id}/download ou /thumbnail */
        String idHero
) {}
