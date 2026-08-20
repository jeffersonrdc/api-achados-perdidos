package br.com.achadosperdidos.controller.dto;

import java.util.List;
import java.util.Map;

/** Snapshot leve para menu/guards do painel (qualquer autenticado). */
public record ConfigRuntimeResponse(
        Map<String, Boolean> modulos,
        boolean triagemObrigatoria,
        List<String> pathsHabilitados
) {}
