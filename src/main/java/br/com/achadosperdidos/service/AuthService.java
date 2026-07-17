package br.com.achadosperdidos.service;

import br.com.achadosperdidos.entity.RefreshToken;
import br.com.achadosperdidos.entity.Usuario;
import br.com.achadosperdidos.repository.RefreshTokenRepository;
import br.com.achadosperdidos.repository.UsuarioRepository;
import br.com.achadosperdidos.security.JwtUtil;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {
    public record AuthTokens(String accessToken, String refreshToken) {}

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final LoginLogService loginLogService;
    private final RefreshTokenRepository refreshTokenRepository;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                       LoginLogService loginLogService, RefreshTokenRepository refreshTokenRepository) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.loginLogService = loginLogService;
        this.refreshTokenRepository = refreshTokenRepository;
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
        return emitirTokens(usuario);
    }

    @Transactional
    public AuthTokens refresh(String refreshToken) {
        if (!jwtUtil.validateRefreshToken(refreshToken)) {
            throw new BadCredentialsException("Refresh token inválido");
        }
        String jti = jwtUtil.getJtiFromToken(refreshToken);
        RefreshToken registro = jti == null ? null : refreshTokenRepository.findByJti(jti).orElse(null);
        if (registro == null || Boolean.TRUE.equals(registro.getFgRevogado())
                || registro.getDtExpiracao().isBefore(LocalDateTime.now())) {
            throw new BadCredentialsException("Refresh token inválido ou revogado");
        }
        String subject = jwtUtil.getSubjectFromToken(refreshToken);
        Usuario usuario = subject == null ? null : usuarioRepository.findByNmEmail(subject).orElse(null);
        if (usuario == null || !Boolean.TRUE.equals(usuario.getFgAtivo()) || Boolean.TRUE.equals(usuario.getFgExcluido())) {
            throw new BadCredentialsException("Refresh token inválido");
        }
        // Rotação: o token usado é revogado e um novo par é emitido.
        revogar(registro);
        return emitirTokens(usuario);
    }

    /** Logout real: revoga o refresh token informado. Idempotente. */
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) return;
        String jti = jwtUtil.getJtiFromToken(refreshToken);
        if (jti == null) return;
        refreshTokenRepository.findByJti(jti)
                .filter(rt -> !Boolean.TRUE.equals(rt.getFgRevogado()))
                .ifPresent(this::revogar);
    }

    /** Gera o par access/refresh e persiste o refresh token para permitir revogação. */
    private AuthTokens emitirTokens(Usuario usuario) {
        String subject = usuario.getNmEmail();
        String accessToken = jwtUtil.generateAccessToken(subject);
        String refreshToken = jwtUtil.generateRefreshToken(subject);

        RefreshToken registro = new RefreshToken();
        registro.setJti(jwtUtil.getJtiFromToken(refreshToken));
        registro.setUsuario(usuario);
        registro.setDtExpiracao(jwtUtil.getExpirationFromToken(refreshToken));
        registro.setFgRevogado(false);
        registro.setDtCadastro(LocalDateTime.now());
        refreshTokenRepository.save(registro);

        return new AuthTokens(accessToken, refreshToken);
    }

    private void revogar(RefreshToken registro) {
        registro.setFgRevogado(true);
        registro.setDtRevogacao(LocalDateTime.now());
        refreshTokenRepository.save(registro);
    }
}
