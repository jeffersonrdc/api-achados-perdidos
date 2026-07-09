package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SlaRegraCreateRequest(
        String idEvento,
        @NotBlank String tpProcesso,
        @NotNull Integer qtHorasLimite,
        Integer qtHorasAlerta,
        Boolean fgEnviarAlerta,
        String dsObservacao
) {}
