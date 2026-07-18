package br.com.achadosperdidos.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Resolve o IP do cliente de forma segura.
 * Com {@code server.forward-headers-strategy=NATIVE}, o Tomcat já aplica
 * X-Forwarded-For apenas quando a origem é um proxy confiável — portanto
 * usamos {@link HttpServletRequest#getRemoteAddr()} e nunca o header cru.
 */
@Component
public class ClientIpResolver {

    public String resolve(HttpServletRequest request) {
        if (request == null) return null;
        return IpAddressUtil.normalize(request.getRemoteAddr());
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
