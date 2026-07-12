package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record EquipeCreateRequest(
        @NotBlank String idEvento,
        @NotBlank String nmEquipe,
        @NotBlank String tpEquipe,
        String idLocal,
        String dsResponsabilidade
) {}
