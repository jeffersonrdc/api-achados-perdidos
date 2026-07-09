package br.com.achadosperdidos.service;

import br.com.achadosperdidos.entity.Usuario;
import br.com.achadosperdidos.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioContextService {
    private final UsuarioRepository usuarioRepository;
    public UsuarioContextService(UsuarioRepository usuarioRepository) { this.usuarioRepository = usuarioRepository; }

    @Transactional(readOnly = true)
    public Usuario requireUsuarioLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            throw new IllegalStateException("Usuário não autenticado.");
        }
        return usuarioRepository.findWithPerfilByNmEmail(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Usuário não encontrado."));
    }

    @Transactional(readOnly = true)
    public Long requireUsuarioLogadoId() { return requireUsuarioLogado().getId(); }
}
