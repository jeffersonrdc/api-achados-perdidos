package br.com.achadosperdidos.service;

import br.com.achadosperdidos.util.ClientIpResolver;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

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
    private final ClientIpResolver clientIpResolver;

    public AuditoriaContextService(UsuarioContextService usuarioContextService,
                                   ClientIpResolver clientIpResolver) {
        this.usuarioContextService = usuarioContextService;
        this.clientIpResolver = clientIpResolver;
    }

    public void marcarContexto() {
        em.createNativeQuery("SET @app_user_id = :uid, @app_ip = :ip")
                .setParameter("uid", usuarioIdOuZero())
                .setParameter("ip", clientIpResolver.resolveCurrentOrEmpty())
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
}
