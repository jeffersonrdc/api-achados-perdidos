package br.com.achadosperdidos.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Resolve a URL base pública do portal para CTAs de e-mail.
 * Prioridade: {@code app.portal.base-url} (se não for localhost) → host da requisição
 * ({@code X-Forwarded-*}) → fallback localhost:4300.
 */
@Service
public class PortalBaseUrlService {

    private static final String FALLBACK = "http://localhost:4300";

    @Value("${app.portal.base-url:}")
    private String configuredBaseUrl;

    public String resolve() {
        String configured = configuredBaseUrl == null ? "" : configuredBaseUrl.trim();
        if (!configured.isBlank() && !isLocalhost(configured)) {
            return stripTrailingSlash(configured);
        }
        String fromRequest = resolveFromRequest();
        if (fromRequest != null && !fromRequest.isBlank()) {
            return stripTrailingSlash(fromRequest);
        }
        if (!configured.isBlank()) {
            return stripTrailingSlash(configured);
        }
        return FALLBACK;
    }

    private String resolveFromRequest() {
        try {
            var attrs = RequestContextHolder.getRequestAttributes();
            if (!(attrs instanceof ServletRequestAttributes sra)) {
                return null;
            }
            HttpServletRequest req = sra.getRequest();
            if (req == null) return null;

            String proto = firstHeader(req, "X-Forwarded-Proto");
            if (proto == null || proto.isBlank()) {
                proto = req.getScheme();
            } else {
                proto = proto.split(",")[0].trim();
            }

            String host = firstHeader(req, "X-Forwarded-Host");
            if (host == null || host.isBlank()) {
                host = req.getServerName();
                int port = req.getServerPort();
                boolean defaultPort = ("http".equalsIgnoreCase(proto) && port == 80)
                        || ("https".equalsIgnoreCase(proto) && port == 443);
                if (!defaultPort && port > 0) {
                    host = host + ":" + port;
                }
            } else {
                host = host.split(",")[0].trim();
            }

            if (host == null || host.isBlank()) return null;
            return proto + "://" + host;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String firstHeader(HttpServletRequest req, String name) {
        String v = req.getHeader(name);
        return v == null || v.isBlank() ? null : v;
    }

    private static boolean isLocalhost(String url) {
        String lower = url.toLowerCase();
        return lower.contains("localhost") || lower.contains("127.0.0.1");
    }

    private static String stripTrailingSlash(String url) {
        String u = url.trim();
        while (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        return u;
    }
}
