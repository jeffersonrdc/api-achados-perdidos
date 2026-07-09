package br.com.achadosperdidos.service;

import br.com.achadosperdidos.entity.Usuario;
import br.com.achadosperdidos.repository.UsuarioRepository;
import br.com.achadosperdidos.security.JwtUtil;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    public record AuthTokens(String accessToken, String refreshToken) {}

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional(readOnly = true)
    public AuthTokens authenticate(String identificador, String senha) {
        Usuario usuario = usuarioRepository.findWithPerfilByNmEmail(identificador.trim())
                .or(() -> usuarioRepository.findWithPerfilByNmLogin(identificador.trim()))
                .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));
        if (!passwordEncoder.matches(senha, usuario.getNmSenha())) {
            throw new BadCredentialsException("Credenciais inválidas");
        }
        if (!Boolean.TRUE.equals(usuario.getFgAtivo()) || Boolean.TRUE.equals(usuario.getFgExcluido())) {
            throw new BadCredentialsException("Usuário inativo");
        }
        String subject = usuario.getNmEmail();
        return new AuthTokens(jwtUtil.generateAccessToken(subject), jwtUtil.generateRefreshToken(subject));
    }

    @Transactional(readOnly = true)
    public AuthTokens refresh(String refreshToken) {
        if (!jwtUtil.validateRefreshToken(refreshToken)) {
            throw new BadCredentialsException("Refresh token inválido");
        }
        String subject = jwtUtil.getSubjectFromToken(refreshToken);
        if (subject == null || usuarioRepository.findByNmEmail(subject).isEmpty()) {
            throw new BadCredentialsException("Refresh token inválido");
        }
        return new AuthTokens(jwtUtil.generateAccessToken(subject), jwtUtil.generateRefreshToken(subject));
    }
}
