package br.com.achadosperdidos.controller.dto;

import java.time.LocalDateTime;

public record EventoResponse(
        String id,
        String nmEvento,
        String dsEvento,
        LocalDateTime dtInicio,
        LocalDateTime dtFim,
        String nmLocal,
        String nmCidade,
        String sgUf,
        Integer qtDiasRetencao,
        Boolean fgAtivo,
        /** ID assinado do arquivo LOGO no S3 (via /arquivos/{id}/download). */
        String idLogo,
        /** ID assinado do arquivo HERO no S3. */
        String idHero
) {}
