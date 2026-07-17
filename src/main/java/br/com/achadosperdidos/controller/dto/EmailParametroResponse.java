package br.com.achadosperdidos.controller.dto;

/** Mapeamento propósito -> conta/template para os selects de parâmetros. */
public record EmailParametroResponse(
        String tpEvento,
        String idEmailConfig,
        String nmConfig,
        String nmTemplate,
        String nmAssunto
) {}
