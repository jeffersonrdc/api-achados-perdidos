package br.com.achadosperdidos.entity;

public enum PerfilCodigo {
    ADMINISTRADOR, OPERADOR, ATENDENTE, CONSULTA;

    public String roleName() { return "ROLE_" + name(); }

    public static PerfilCodigo fromNmPerfil(String nmPerfil) {
        if (nmPerfil == null || nmPerfil.isBlank()) throw new IllegalArgumentException("Perfil inválido.");
        return PerfilCodigo.valueOf(nmPerfil.trim().toUpperCase().replace(' ', '_'));
    }
}
