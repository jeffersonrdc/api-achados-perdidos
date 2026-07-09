package br.com.achadosperdidos.service;

import br.com.achadosperdidos.entity.PerfilCodigo;
import br.com.achadosperdidos.entity.Usuario;
import br.com.achadosperdidos.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UsuarioRepository usuarioRepository;
    public CustomUserDetailsService(UsuarioRepository usuarioRepository) { this.usuarioRepository = usuarioRepository; }

    @Override @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findWithPerfilByNmEmail(username)
                .or(() -> usuarioRepository.findWithPerfilByNmLogin(username))
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
        String role = PerfilCodigo.fromNmPerfil(usuario.getPerfil().getNmPerfil()).roleName();
        return User.builder()
                .username(usuario.getNmEmail())
                .password(usuario.getNmSenha())
                .disabled(!Boolean.TRUE.equals(usuario.getFgAtivo()) || Boolean.TRUE.equals(usuario.getFgExcluido()))
                .authorities(role)
                .build();
    }
}
