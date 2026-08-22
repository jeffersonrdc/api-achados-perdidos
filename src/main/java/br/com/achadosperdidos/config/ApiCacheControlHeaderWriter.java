package br.com.achadosperdidos.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.header.HeaderWriter;
import org.springframework.stereotype.Component;

/**
 * Cache-Control da API, pensado para a CDN (CloudFront) em produção.
 *
 * <p>Três políticas:</p>
 * <ul>
 *   <li><b>GETs públicos do portal</b> (200 OK): TTL positivo, para a borda absorver
 *       catálogo e taxonomia em pico de evento;</li>
 *   <li><b>binários</b> ({@code /arquivos/**}): o controller já define {@code public}/
 *       {@code private} com TTL próprio — preservamos;</li>
 *   <li><b>todo o resto</b>: {@code no-store} — rotas autenticadas, POSTs, tokens
 *       pessoais (devoluções/respostas) e <b>qualquer resposta de erro</b>.</li>
 * </ul>
 *
 * <p>É um {@link HeaderWriter} e não um {@code Filter} de propósito. O
 * {@code HeaderWriterFilter} do Spring Security embrulha a resposta e invoca os writers
 * no <i>commit</i> — o único momento em que (a) o status já está definido e (b) os headers
 * ainda não foram enviados. Um filtro que escrevesse depois de {@code chain.doFilter()}
 * perderia toda resposta que já tivesse commitado (corpo acima do buffer de 8 KB do
 * Tomcat: catálogo paginado, listagens grandes do painel), e essas sairiam <b>sem</b>
 * {@code no-store} — já que o {@code cacheControl} nativo foi desligado no
 * {@link SecurityConfig} em favor desta classe.</p>
 */
@Component
public class ApiCacheControlHeaderWriter implements HeaderWriter {

    /**
     * Desligue ({@code false}) em ambiente <b>sem CDN</b> ou como alavanca de emergência:
     * todo GET público volta a responder {@code no-store}, exatamente como antes desta
     * política existir. Útil em DEV, onde o cache do navegador atrasaria em até 5 min a
     * visualização de um cadastro recém-alterado no painel.
     */
    private final boolean habilitado;

    public ApiCacheControlHeaderWriter(
            @Value("${app.cache.public-json.enabled:true}") boolean habilitado) {
        this.habilitado = habilitado;
    }

    /** Catálogo e listas que mudam durante o evento — alívio de pico sem atraso longo. */
    static final String PUBLIC_JSON_CURTO = "public, max-age=60, stale-while-revalidate=300";
    static final long PUBLIC_JSON_CURTO_SEGUNDOS = 60L;

    /** Cadastros de referência (categorias, cores, etc.) mudam pouco. */
    static final String PUBLIC_JSON_REF = "public, max-age=300, stale-while-revalidate=600";
    static final long PUBLIC_JSON_REF_SEGUNDOS = 300L;

    static final String NO_STORE = "no-cache, no-store, max-age=0, must-revalidate";

    @Override
    public void writeHeaders(HttpServletRequest request, HttpServletResponse response) {
        if (temPoliticaExplicita(response.getHeader(HttpHeaders.CACHE_CONTROL))) {
            // download/thumbnail (portal public ou painel private) já definiram TTL próprio
            return;
        }
        String path = request.getRequestURI();
        if (path != null && podeCachearPublico(request, response, path)) {
            aplicarPublico(response, path);
            return;
        }
        aplicarNoStore(response);
    }

    /**
     * Só cacheia publicamente resposta de sucesso. Erro (404 de evento inexistente, 405,
     * 429 do rate limit, 500 de falha transitória) nunca vai para a borda: um 500 cacheado
     * por 60s com {@code stale-while-revalidate} continuaria sendo servido a todos mesmo
     * depois de o pod se recuperar.
     */
    private boolean podeCachearPublico(HttpServletRequest request, HttpServletResponse response, String path) {
        return habilitado
                && HttpMethod.GET.matches(request.getMethod())
                && response.getStatus() == HttpServletResponse.SC_OK
                && isPortalPublicJsonGet(path);
    }

    /** Controller já definiu public/private (não sobrescrever com no-store). */
    static boolean temPoliticaExplicita(String cacheControl) {
        if (cacheControl == null || cacheControl.isBlank()) {
            return false;
        }
        String c = cacheControl.toLowerCase();
        return c.contains("public") || c.contains("private");
    }

    /**
     * GETs públicos de listagem/consulta do portal (não imagens, não token personalizado).
     */
    static boolean isPortalPublicJsonGet(String path) {
        if (!path.startsWith("/api/v1/portal/")) {
            return false;
        }
        // Personalizado / token — nunca cache público
        if (path.contains("/meus-claims") || path.contains("/respostas/") || path.contains("/devolucoes/")) {
            return false;
        }
        // Binários: Cache-Control já vem do controller
        if (path.contains("/arquivos/")) {
            return false;
        }
        return path.equals("/api/v1/portal/eventos")
                || path.startsWith("/api/v1/portal/eventos/")
                || path.equals("/api/v1/portal/status")
                || path.equals("/api/v1/portal/metricas")
                || path.equals("/api/v1/portal/contatos")
                || path.equals("/api/v1/portal/categorias")
                || path.startsWith("/api/v1/portal/categorias/")
                || path.startsWith("/api/v1/portal/subcategorias/")
                || path.equals("/api/v1/portal/marcas")
                || path.equals("/api/v1/portal/modelos")
                || path.equals("/api/v1/portal/cores")
                || path.equals("/api/v1/portal/estados");
    }

    static boolean isReferenciaEstatica(String path) {
        return path.equals("/api/v1/portal/categorias")
                || path.startsWith("/api/v1/portal/categorias/")
                || path.startsWith("/api/v1/portal/subcategorias/")
                || path.equals("/api/v1/portal/marcas")
                || path.equals("/api/v1/portal/modelos")
                || path.equals("/api/v1/portal/cores")
                || path.equals("/api/v1/portal/estados")
                || path.matches("/api/v1/portal/eventos/[^/]+/locais");
    }

    private static void aplicarPublico(HttpServletResponse response, String path) {
        boolean referencia = isReferenciaEstatica(path);
        String cacheControl = referencia ? PUBLIC_JSON_REF : PUBLIC_JSON_CURTO;
        long segundos = referencia ? PUBLIC_JSON_REF_SEGUNDOS : PUBLIC_JSON_CURTO_SEGUNDOS;
        response.setHeader(HttpHeaders.CACHE_CONTROL, cacheControl);
        response.setDateHeader(HttpHeaders.EXPIRES, System.currentTimeMillis() + segundos * 1000L);
    }

    private static void aplicarNoStore(HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, NO_STORE);
        response.setHeader(HttpHeaders.PRAGMA, "no-cache");
        response.setDateHeader(HttpHeaders.EXPIRES, 0);
    }
}
