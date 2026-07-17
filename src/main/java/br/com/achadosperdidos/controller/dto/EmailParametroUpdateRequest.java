package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;

/** Atualiza qual conta/assunto é usada em um propósito (email_parametro). */
public record EmailParametroUpdateRequest(
        @NotBlank String tpEvento,
        String idEmailConfig,
        String nmAssunto
) {}
