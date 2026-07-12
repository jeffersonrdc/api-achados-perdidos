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
    private final LoginLogService loginLogService;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                       LoginLogService loginLogService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.loginLogService = loginLogService;
    }

    public AuthTokens authenticate(String identificador, String senha) {
        return authenticate(identificador, senha, null, null, null);
    }

    // Transacao read-write unica: a leitura do usuario e a gravacao do login_log
    // acontecem na mesma transacao. Evita o conflito de FK entre transacoes
    // (login_log -> usuario) que ocorreria com REQUIRES_NEW.
    @Transactional
    public AuthTokens authenticate(String identificador, String senha, String ip, String dispositivo, String navegador) {
        Usuario usuario = usuarioRepository.findWithPerfilByNmEmail(identificador.trim())
                .or(() -> usuarioRepository.findWithPerfilByNmLogin(identificador.trim()))
                .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));
        if (!passwordEncoder.matches(senha, usuario.getNmSenha())) {
            throw new BadCredentialsException("Credenciais inválidas");
        }
        if (!Boolean.TRUE.equals(usuario.getFgAtivo()) || Boolean.TRUE.equals(usuario.getFgExcluido())) {
            throw new BadCredentialsException("Usuário inativo");
        }
        // Auditoria de acesso (transacao independente, nunca bloqueia o login).
        loginLogService.registrarAcesso(usuario.getId(), ip, dispositivo, navegador);
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
