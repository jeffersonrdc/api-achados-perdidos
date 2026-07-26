package br.com.achadosperdidos.controller.dto;

/**
 * @param emailEnviado preenchido no cadastro (null nas demais respostas)
 * @param emailAviso   motivo quando o e-mail de credenciais não foi enviado
 */
public record UsuarioResponse(
        String id,
        String nmUsuario,
        String nmLogin,
        String nmEmail,
        String nmPerfil,
        Boolean fgAtivo,
        Boolean emailEnviado,
        String emailAviso
) {
    /** Resposta sem metadados de e-mail (listagem / edição / etc.). */
    public static UsuarioResponse of(
            String id, String nmUsuario, String nmLogin, String nmEmail, String nmPerfil, Boolean fgAtivo) {
        return new UsuarioResponse(id, nmUsuario, nmLogin, nmEmail, nmPerfil, fgAtivo, null, null);
    }
}
