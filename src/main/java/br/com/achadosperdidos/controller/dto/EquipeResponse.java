package br.com.achadosperdidos.controller.dto;

import java.util.List;

public record EquipeResponse(
        String id,
        String idEvento,
        String nmEquipe,
        String tpEquipe,
        String idLocal,
        String nmLocal,
        String dsResponsabilidade,
        Boolean fgAtivo,
        List<EquipeMembroResponse> membros
) {}
