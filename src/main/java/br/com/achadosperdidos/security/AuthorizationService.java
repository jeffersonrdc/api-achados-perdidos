package br.com.achadosperdidos.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Avaliador de permissoes usado nos @PreAuthorize: {@code @authz.pode('modulo.acao')}.
 * Administrador (ROLE_ADMIN) tem override e passa em qualquer checagem, evitando
 * lockout mesmo com permissoes mal configuradas.
 */
@Component("authz")
public class AuthorizationService {

    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    public boolean pode(String permissao) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        for (GrantedAuthority a : auth.getAuthorities()) {
            String value = a.getAuthority();
            if (ROLE_ADMIN.equals(value) || permissao.equals(value)) return true;
        }
        return false;
    }

    public boolean podeQualquer(String... permissoes) {
        for (String p : permissoes) {
            if (pode(p)) return true;
        }
        return false;
    }
}
