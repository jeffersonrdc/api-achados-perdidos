package br.com.achadosperdidos.service;

import br.com.achadosperdidos.entity.PerfilCodigo;
import br.com.achadosperdidos.entity.Usuario;
import br.com.achadosperdidos.repository.PermissaoRepository;
import br.com.achadosperdidos.repository.UsuarioRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UsuarioRepository usuarioRepository;
    private final PermissaoRepository permissaoRepository;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository, PermissaoRepository permissaoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.permissaoRepository = permissaoRepository;
    }

    @Override @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findWithPerfilByNmEmail(username)
                .or(() -> usuarioRepository.findWithPerfilByNmLogin(username))
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));

        List<GrantedAuthority> authorities = new ArrayList<>();
        // Role do perfil (mantida para o portal e para o override de admin).
        authorities.add(new SimpleGrantedAuthority(roleDoPerfil(usuario.getPerfil().getNmPerfil())));
        // Permissoes efetivas = permissoes do perfil UNIAO extras do usuario.
        for (String permissao : permissaoRepository.findPermissoesEfetivas(usuario.getId())) {
            authorities.add(new SimpleGrantedAuthority(permissao));
        }

        return User.builder()
                .username(usuario.getNmEmail())
                .password(usuario.getNmSenha())
                .disabled(!Boolean.TRUE.equals(usuario.getFgAtivo()) || Boolean.TRUE.equals(usuario.getFgExcluido()))
                .authorities(authorities)
                .build();
    }

    /** Perfis conhecidos usam o codigo fixo; perfis criados em runtime derivam ROLE_ generico. */
    private String roleDoPerfil(String nmPerfil) {
        try {
            return PerfilCodigo.fromNmPerfil(nmPerfil).roleName();
        } catch (IllegalArgumentException ex) {
            return "ROLE_" + nmPerfil.trim().toUpperCase().replaceAll("[^A-Z0-9]+", "_");
        }
    }
}
