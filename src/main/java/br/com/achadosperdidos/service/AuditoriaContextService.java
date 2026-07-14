package br.com.achadosperdidos.service;

import br.com.achadosperdidos.util.IpAddressUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Publica o usuário logado e o IP da requisição como variáveis de sessão do MySQL
 * (@app_user_id e @app_ip). As triggers de auditoria e a SP_RegistrarAuditoria leem
 * essas variáveis para gravar quem fez a alteração e de qual IP.
 *
 * Deve ser chamado no início de cada operação de escrita, dentro da mesma transação
 * (mesma conexão JDBC) em que o INSERT/UPDATE/DELETE ocorre.
 */
@Service
public class AuditoriaContextService {

    @PersistenceContext
    private EntityManager em;

    private final UsuarioContextService usuarioContextService;

    public AuditoriaContextService(UsuarioContextService usuarioContextService) {
        this.usuarioContextService = usuarioContextService;
    }

    public void marcarContexto() {
        em.createNativeQuery("SET @app_user_id = :uid, @app_ip = :ip")
                .setParameter("uid", usuarioIdOuZero())
                .setParameter("ip", ipRequisicaoOuVazio())
                .executeUpdate();
    }

    private long usuarioIdOuZero() {
        try {
            Long id = usuarioContextService.requireUsuarioLogadoId();
            return id != null ? id : 0L;
        } catch (RuntimeException ex) {
            return 0L;
        }
    }

    private String ipRequisicaoOuVazio() {
        try {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes sra) {
                HttpServletRequest req = sra.getRequest();
                String fwd = req.getHeader("X-Forwarded-For");
                String ip = (fwd != null && !fwd.isBlank()) ? fwd.split(",")[0].trim() : req.getRemoteAddr();
                String normalizado = IpAddressUtil.normalize(ip);
                return normalizado != null ? normalizado : "";
            }
        } catch (RuntimeException ignored) {
            // sem contexto de requisição (ex.: tarefa agendada) — grava sem IP
        }
        return "";
    }
}
