package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record EventoCreateRequest(
        @NotBlank String idEmpresa,
        @NotBlank String nmEvento,
        String dsEvento,
        @NotNull LocalDateTime dtInicio,
        @NotNull LocalDateTime dtFim,
        String nmLocal,
        String nmCidade,
        String sgUf,
        Integer qtDiasRetencao,
        String urlLogo,
        String urlHero
) {}
