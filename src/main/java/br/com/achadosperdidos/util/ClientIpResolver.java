package br.com.achadosperdidos.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Resolve o IP real do cliente (rate limit por IP, auditoria e logs de acesso).
 *
 * <p>Base: {@code server.forward-headers-strategy=NATIVE}, em que o Tomcat
 * (RemoteIpValve) só honra {@code X-Forwarded-For} vindo de proxies confiáveis —
 * por padrão apenas faixas privadas. Isso cobre bem o cenário "apenas Nginx/ALB
 * na frente".</p>
 *
 * <p><b>Com CDN na frente (CloudFront, produção AWS)</b> a cadeia vira
 * {@code XFF: <viewer>, <edge-cloudfront>}. O IP do edge é público, logo o valve
 * não o considera proxy interno e para nele: {@code getRemoteAddr()} passa a
 * devolver o IP do <i>edge</i>, e não o do usuário. O efeito é todo mundo atrás
 * do mesmo edge cair no mesmo balde de rate limit e a auditoria registrar o IP
 * errado.</p>
 *
 * <p>Para esse caso existe {@code app.security.client-ip.trusted-header}: quando
 * configurado (ex.: {@code CloudFront-Viewer-Address}), o valor do header tem
 * precedência. O CloudFront <b>sobrescreve</b> esse header em toda requisição, então
 * ele não é falsificável — <b>desde que a origem só seja alcançável através da CDN</b>.
 * Por isso o default é vazio (desligado): habilite apenas onde essa premissa vale.</p>
 */
@Component
public class ClientIpResolver {

    private final String trustedHeader;

    public ClientIpResolver(
            @Value("${app.security.client-ip.trusted-header:}") String trustedHeader) {
        this.trustedHeader = trustedHeader == null ? "" : trustedHeader.trim();
    }

    public String resolve(HttpServletRequest request) {
        if (request == null) return null;
        String doHeader = doHeaderConfiavel(request);
        if (doHeader != null) {
            return doHeader;
        }
        return IpAddressUtil.normalize(request.getRemoteAddr());
    }

    /**
     * IP declarado pelo header confiável, se habilitado e presente.
     * Aceita tanto {@code IP:porta} (CloudFront-Viewer-Address) quanto lista
     * separada por vírgula (X-Forwarded-For), em que o primeiro item é o viewer.
     */
    private String doHeaderConfiavel(HttpServletRequest request) {
        if (trustedHeader.isEmpty()) {
            return null;
        }
        String bruto = request.getHeader(trustedHeader);
        if (bruto == null || bruto.isBlank()) {
            return null;
        }
        String primeiro = bruto.split(",")[0].trim();
        if (primeiro.isEmpty()) {
            return null;
        }
        return IpAddressUtil.normalize(IpAddressUtil.stripPort(primeiro));
    }

    /** IP da requisição atual no thread, ou {@code null} fora de contexto HTTP. */
    public String resolveCurrent() {
        try {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes sra) {
                return resolve(sra.getRequest());
            }
        } catch (RuntimeException ignored) {
            // tarefa agendada / sem request
        }
        return null;
    }

    public String resolveCurrentOrEmpty() {
        String ip = resolveCurrent();
        return ip != null ? ip : "";
    }
}
