package br.com.achadosperdidos.service;

import br.com.achadosperdidos.entity.Usuario;
import br.com.achadosperdidos.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UsuarioContextService {
    private final UsuarioRepository usuarioRepository;
    public UsuarioContextService(UsuarioRepository usuarioRepository) { this.usuarioRepository = usuarioRepository; }

    @Transactional(readOnly = true)
    public Usuario requireUsuarioLogado() {
        return findUsuarioLogado()
                .orElseThrow(() -> new IllegalStateException("Usuário não autenticado."));
    }

    /**
     * Consulta o usuário autenticado sem lançar exceção.
     * Preferível em fluxos públicos/opcionais: lançar RuntimeException dentro de
     * {@code @Transactional} marca a transação como rollback-only mesmo se o caller
     * capturar a exceção (UnexpectedRollbackException no commit).
     */
    @Transactional(readOnly = true)
    public Optional<Usuario> findUsuarioLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null
                || "anonymousUser".equals(auth.getName())) {
            return Optional.empty();
        }
        return usuarioRepository.findWithPerfilByNmEmail(auth.getName());
    }

    @Transactional(readOnly = true)
    public Long requireUsuarioLogadoId() { return requireUsuarioLogado().getId(); }
}
