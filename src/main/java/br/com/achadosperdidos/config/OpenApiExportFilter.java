package br.com.achadosperdidos.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Alinha o Swagger da API ao padrão dos frontends:
 * barra superior com "Abrir JSON" e "Exportar JSON".
 * Em {@code /api-docs/**?export=1}, força download do arquivo.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class OpenApiExportFilter extends OncePerRequestFilter {

    private static final String EXPORT_UI = """
            <style id="ap-swagger-bar-style">
              #ap-swagger-bar {
                display: flex;
                align-items: center;
                flex-wrap: wrap;
                gap: 0.75rem 1rem;
                padding: 0.75rem 1.25rem;
                background: #111827;
                color: #f9fafb;
                font-family: system-ui, sans-serif;
                font-size: 0.95rem;
                position: sticky;
                top: 0;
                z-index: 10000;
                border-bottom: 1px solid #1f2937;
              }
              #ap-swagger-bar strong { font-weight: 600; }
              #ap-swagger-bar .ap-actions {
                margin-left: auto;
                display: flex;
                gap: 0.5rem;
                flex-wrap: wrap;
              }
              #ap-swagger-bar .ap-export {
                display: inline-flex;
                align-items: center;
                padding: 0.35rem 0.75rem;
                border-radius: 0.375rem;
                border: 1px solid #4b5563;
                background: #1f2937;
                color: #e5e7eb;
                font-size: 0.85rem;
                text-decoration: none;
                cursor: pointer;
                font: inherit;
              }
              #ap-swagger-bar .ap-export:hover {
                background: #374151;
                border-color: #6b7280;
              }
              body { margin: 0; }
            </style>
            <header id="ap-swagger-bar">
              <strong>Documentação da API — Achados e Perdidos</strong>
              <div class="ap-actions">
                <a id="ap-open-json" class="ap-export" href="/api-docs/01-painel-administrativo" target="_blank" rel="noopener">Abrir JSON</a>
                <button id="ap-export-json" type="button" class="ap-export">Exportar JSON</button>
              </div>
            </header>
            <script>
            (function () {
              function currentSpecUrl() {
                var select = document.querySelector('.topbar-wrapper select, #select, .download-url-wrapper select');
                if (select && select.value) {
                  var v = String(select.value).trim();
                  if (v.startsWith('http') || v.startsWith('/')) return v;
                  if (v.startsWith('./')) return '/' + v.slice(2);
                  return '/api-docs/' + v;
                }
                var link = document.querySelector('.information-container a[href*="api-docs"], .swagger-ui a[href*="api-docs"]');
                if (link && link.getAttribute('href')) {
                  var href = link.getAttribute('href');
                  if (href.startsWith('http') || href.startsWith('/')) return href;
                  if (href.startsWith('./')) return '/' + href.slice(2);
                }
                return '/api-docs/01-painel-administrativo';
              }
              function filenameFromUrl(url) {
                try {
                  var path = url.split('?')[0];
                  var parts = path.split('/').filter(Boolean);
                  var last = parts[parts.length - 1] || 'openapi';
                  return last + '.openapi.json';
                } catch (e) {
                  return 'openapi.json';
                }
              }
              function syncOpenLink() {
                var a = document.getElementById('ap-open-json');
                if (a) a.href = currentSpecUrl();
              }
              async function exportJson() {
                var url = currentSpecUrl();
                var sep = url.indexOf('?') >= 0 ? '&' : '?';
                var exportUrl = url + sep + 'export=1';
                try {
                  var resp = await fetch(exportUrl);
                  if (!resp.ok) throw new Error('Falha ao baixar a especificação');
                  var blob = await resp.blob();
                  var obj = URL.createObjectURL(blob);
                  var a = document.createElement('a');
                  a.href = obj;
                  a.download = filenameFromUrl(url);
                  document.body.appendChild(a);
                  a.click();
                  a.remove();
                  URL.revokeObjectURL(obj);
                } catch (e) {
                  window.location.href = exportUrl;
                }
              }
              function bind() {
                syncOpenLink();
                var btn = document.getElementById('ap-export-json');
                if (btn && !btn.dataset.bound) {
                  btn.dataset.bound = '1';
                  btn.addEventListener('click', exportJson);
                }
                var select = document.querySelector('.topbar-wrapper select, #select, .download-url-wrapper select');
                if (select && !select.dataset.apBound) {
                  select.dataset.apBound = '1';
                  select.addEventListener('change', syncOpenLink);
                }
              }
              var tries = 0;
              var timer = setInterval(function () {
                tries += 1;
                bind();
                if (tries > 80) clearInterval(timer);
              }, 250);
            })();
            </script>
            """;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        boolean apiDocs = uri.equals("/api-docs") || uri.startsWith("/api-docs/");
        boolean exportJson = apiDocs && "1".equals(request.getParameter("export"));
        boolean swaggerHtml = uri.contains("/swagger-ui/") && uri.endsWith(".html");

        if (exportJson) {
            String group = "openapi";
            if (uri.startsWith("/api-docs/") && uri.length() > "/api-docs/".length()) {
                group = uri.substring("/api-docs/".length()).replace('/', '-');
            }
            if (group.isBlank()) group = "openapi";
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + group + ".openapi.json\"");
            filterChain.doFilter(request, response);
            return;
        }

        if (!swaggerHtml) {
            filterChain.doFilter(request, response);
            return;
        }

        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
        filterChain.doFilter(request, wrapper);
        byte[] body = wrapper.getContentAsByteArray();
        String contentType = wrapper.getContentType();
        if (contentType != null && contentType.contains("text/html") && body.length > 0) {
            String html = new String(body, StandardCharsets.UTF_8);
            if (!html.contains("ap-swagger-bar") && html.contains("<body")) {
                html = html.replaceFirst("(?i)<body([^>]*)>", "<body$1>" + EXPORT_UI);
                byte[] out = html.getBytes(StandardCharsets.UTF_8);
                response.setContentLength(out.length);
                response.getOutputStream().write(out);
                return;
            }
        }
        wrapper.copyBodyToResponse();
    }
}
