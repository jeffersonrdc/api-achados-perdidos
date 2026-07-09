package br.com.achadosperdidos.entity;

public enum PerfilCodigo {
    ADMINISTRADOR, OPERADOR, ATENDENTE, CONSULTA, PARTICIPANTE;

    public String roleName() {
        return switch (this) {
            case ADMINISTRADOR -> "ROLE_ADMIN";
            case OPERADOR -> "ROLE_OPERADOR";
            case ATENDENTE -> "ROLE_ATENDENTE";
            case CONSULTA -> "ROLE_CONSULTA";
            case PARTICIPANTE -> "ROLE_PARTICIPANTE";
        };
    }

    public static PerfilCodigo fromNmPerfil(String nmPerfil) {
        if (nmPerfil == null || nmPerfil.isBlank()) throw new IllegalArgumentException("Perfil inválido.");
        return PerfilCodigo.valueOf(nmPerfil.trim().toUpperCase().replace(' ', '_'));
    }
}
