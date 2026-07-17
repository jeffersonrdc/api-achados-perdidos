package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;

/** Cadastro/edição de uma conta SMTP (email_config). */
public record EmailConfigRequest(
        @NotBlank String nmConfig,
        String nmHost,
        Integer nrPorta,
        String nmUsuario,
        String nmSenha,
        String nmRemetente,
        String nmRemetenteNome,
        Boolean fgTls,
        Boolean fgAtivo
) {}
