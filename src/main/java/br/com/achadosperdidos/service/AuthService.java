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
    private final AuthEventService authEventService;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                       LoginLogService loginLogService, RefreshTokenRepository refreshTokenRepository,
                       AuthEventService authEventService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.loginLogService = loginLogService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.authEventService = authEventService;
    }

    public AuthTokens authenticate(String identificador, String senha) {
        return authenticate(identificador, senha, null, null, null);
    }

    @Transactional
    public AuthTokens authenticate(String identificador, String senha, String ip, String dispositivo, String navegador) {
        Usuario usuario = usuarioRepository.findWithPerfilByNmEmail(identificador.trim())
                .or(() -> usuarioRepository.findWithPerfilByNmLogin(identificador.trim()))
                .orElse(null);
        if (usuario == null) {
            // Sem IDR_Usuario: REQUIRES_NEW não compete com locks da TX atual (ex.: testes @Transactional).
            authEventService.registrarIndependente(
                    AuthEventService.LOGIN_CREDENCIAL_INVALIDA, AuthEventService.RESULTADO_FALHA,
                    "USUARIO_INEXISTENTE", null, identificador, ip, dispositivo, navegador);
            throw new BadCredentialsException("Credenciais inválidas");
        }
        if (!passwordEncoder.matches(senha, usuario.getNmSenha())) {
            authEventService.registrarIndependente(
                    AuthEventService.LOGIN_CREDENCIAL_INVALIDA, AuthEventService.RESULTADO_FALHA,
                    "SENHA_INVALIDA", null, identificador, ip, dispositivo, navegador);
            throw new BadCredentialsException("Credenciais inválidas");
        }
        if (!Boolean.TRUE.equals(usuario.getFgAtivo()) || Boolean.TRUE.equals(usuario.getFgExcluido())) {
            authEventService.registrarIndependente(
                    AuthEventService.LOGIN_CREDENCIAL_INVALIDA, AuthEventService.RESULTADO_FALHA,
                    "USUARIO_INATIVO", null, identificador, ip, dispositivo, navegador);
            throw new BadCredentialsException("Credenciais inválidas");
        }
        loginLogService.registrarAcesso(usuario.getId(), ip, dispositivo, navegador);
        authEventService.registrar(
                AuthEventService.LOGIN_SUCESSO, AuthEventService.RESULTADO_SUCESSO,
                null, usuario.getId(), identificador, ip, dispositivo, navegador);
        return emitirTokens(usuario);
    }

    @Transactional
    public AuthTokens refresh(String refreshToken, String ip, String dispositivo, String navegador) {
        try {
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
            revogar(registro);
            AuthTokens tokens = emitirTokens(usuario);
            authEventService.registrar(
                    AuthEventService.REFRESH_SUCESSO, AuthEventService.RESULTADO_SUCESSO,
                    null, usuario.getId(), subject, ip, dispositivo, navegador);
            return tokens;
        } catch (BadCredentialsException ex) {
            authEventService.registrarIndependente(
                    AuthEventService.REFRESH_INVALIDO, AuthEventService.RESULTADO_FALHA,
                    "REFRESH_INVALIDO", null, null, ip, dispositivo, navegador);
            throw ex;
        }
    }

    /** Compatibilidade: refresh sem metadados de rede. */
    @Transactional
    public AuthTokens refresh(String refreshToken) {
        return refresh(refreshToken, null, null, null);
    }

    /** Logout real: revoga o refresh token informado. Idempotente. */
    @Transactional
    public void logout(String refreshToken, String ip, String dispositivo, String navegador) {
        if (refreshToken == null || refreshToken.isBlank()) return;
        String jti = jwtUtil.getJtiFromToken(refreshToken);
        if (jti == null) return;
        refreshTokenRepository.findByJti(jti)
                .filter(rt -> !Boolean.TRUE.equals(rt.getFgRevogado()))
                .ifPresent(rt -> {
                    Long usuarioId = rt.getUsuario() != null ? rt.getUsuario().getId() : null;
                    String email = rt.getUsuario() != null ? rt.getUsuario().getNmEmail() : null;
                    revogar(rt);
                    authEventService.registrar(
                            AuthEventService.LOGOUT, AuthEventService.RESULTADO_SUCESSO,
                            null, usuarioId, email, ip, dispositivo, navegador);
                });
    }

    @Transactional
    public void logout(String refreshToken) {
        logout(refreshToken, null, null, null);
    }

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
