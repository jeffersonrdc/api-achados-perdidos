package br.com.achadosperdidos.controller.dto;

/** Conta SMTP para a UI. A senha nunca é devolvida (fgTemSenha indica se há uma cadastrada). */
public record EmailConfigResponse(
        String id,
        String nmConfig,
        String nmHost,
        Integer nrPorta,
        String nmUsuario,
        String nmRemetente,
        String nmRemetenteNome,
        Boolean fgTls,
        Boolean fgAtivo,
        Boolean fgTemSenha
) {}
