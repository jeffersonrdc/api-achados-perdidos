package br.com.achadosperdidos.controller.dto;

public record EquipeUpdateRequest(
        String nmEquipe,
        String tpEquipe,
        String idLocal,
        String dsResponsabilidade,
        Boolean fgAtivo
) {}
