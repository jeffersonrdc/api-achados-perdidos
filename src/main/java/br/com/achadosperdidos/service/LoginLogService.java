package br.com.achadosperdidos.service;

import br.com.achadosperdidos.entity.LoginLog;
import br.com.achadosperdidos.entity.Usuario;
import br.com.achadosperdidos.repository.LoginLogRepository;
import br.com.achadosperdidos.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/** Auditoria de acesso (secao 14 da especificacao): registra logins bem-sucedidos. */
@Service
public class LoginLogService {

    private final LoginLogRepository loginLogRepository;
    private final UsuarioRepository usuarioRepository;

    public LoginLogService(LoginLogRepository loginLogRepository, UsuarioRepository usuarioRepository) {
        this.loginLogRepository = loginLogRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Registra o acesso participando da transacao do chamador (AuthService),
     * de modo que o login_log e o usuario referenciado fiquem consistentes na
     * mesma transacao.
     */
    public void registrarAcesso(Long usuarioId, String ip, String dispositivo, String navegador) {
        Usuario usuario = usuarioRepository.getReferenceById(usuarioId);
        LoginLog l = new LoginLog();
        l.setUsuario(usuario);
        l.setNrIp(ip);
        l.setNmDispositivo(dispositivo);
        l.setNmNavegador(navegador);
        l.setDtLogin(LocalDateTime.now());
        l.setDtCadastro(LocalDateTime.now());
        l.setFgAtivo(true);
        l.setFgExcluido(false);
        loginLogRepository.save(l);
    }
}
