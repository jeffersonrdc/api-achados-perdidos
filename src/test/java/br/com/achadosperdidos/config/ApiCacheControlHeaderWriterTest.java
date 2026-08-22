package br.com.achadosperdidos.config;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class ApiCacheControlHeaderWriterTest {

    private final ApiCacheControlHeaderWriter writer = new ApiCacheControlHeaderWriter(true);

    private static MockHttpServletRequest req(String metodo, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(metodo, path);
        request.setRequestURI(path);
        return request;
    }

    private String cacheControl(String metodo, String path, int status) {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(status);
        writer.writeHeaders(req(metodo, path), response);
        return response.getHeader(HttpHeaders.CACHE_CONTROL);
    }

    @Test
    void reconheceGetsPublicosDoCatalogo() {
        assertTrue(ApiCacheControlHeaderWriter.isPortalPublicJsonGet("/api/v1/portal/eventos"));
        assertTrue(ApiCacheControlHeaderWriter.isPortalPublicJsonGet("/api/v1/portal/eventos/s2.x/itens"));
        assertTrue(ApiCacheControlHeaderWriter.isPortalPublicJsonGet("/api/v1/portal/categorias"));
        assertTrue(ApiCacheControlHeaderWriter.isPortalPublicJsonGet("/api/v1/portal/cores"));
        assertTrue(ApiCacheControlHeaderWriter.isPortalPublicJsonGet("/api/v1/portal/eventos/s2.x/locais"));
    }

    @Test
    void excluiPersonalizadoEBinarios() {
        assertFalse(ApiCacheControlHeaderWriter.isPortalPublicJsonGet("/api/v1/portal/eventos/s2.x/meus-claims"));
        assertFalse(ApiCacheControlHeaderWriter.isPortalPublicJsonGet("/api/v1/portal/respostas/abc"));
        assertFalse(ApiCacheControlHeaderWriter.isPortalPublicJsonGet("/api/v1/portal/devolucoes/abc"));
        assertFalse(ApiCacheControlHeaderWriter.isPortalPublicJsonGet("/api/v1/portal/arquivos/s2.x/download"));
        assertFalse(ApiCacheControlHeaderWriter.isPortalPublicJsonGet("/api/v1/config/runtime"));
        assertFalse(ApiCacheControlHeaderWriter.isPortalPublicJsonGet("/api/v1/itens"));
    }

    @Test
    void referenciaEstaticaComTtlMaior() {
        assertEquals(ApiCacheControlHeaderWriter.PUBLIC_JSON_REF,
                cacheControl("GET", "/api/v1/portal/categorias", 200));
        assertEquals(ApiCacheControlHeaderWriter.PUBLIC_JSON_REF,
                cacheControl("GET", "/api/v1/portal/eventos/s2.abc/locais", 200));
        assertEquals(ApiCacheControlHeaderWriter.PUBLIC_JSON_CURTO,
                cacheControl("GET", "/api/v1/portal/eventos/s2.abc/itens", 200));
    }

    @Test
    void preservaPoliticaExplicitaDoController() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "public, max-age=86400");
        writer.writeHeaders(req("GET", "/api/v1/portal/arquivos/s2.x/thumbnail"), response);
        assertEquals("public, max-age=86400", response.getHeader(HttpHeaders.CACHE_CONTROL));

        response = new MockHttpServletResponse();
        response.setStatus(200);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "private, max-age=86400");
        writer.writeHeaders(req("GET", "/api/v1/arquivos/s2.x/download"), response);
        assertEquals("private, max-age=86400", response.getHeader(HttpHeaders.CACHE_CONTROL));
    }

    @Test
    void naoCacheiaRespostaDeErroEmRotaPublica() {
        for (int status : new int[]{
                HttpServletResponse.SC_NOT_FOUND,
                HttpServletResponse.SC_BAD_REQUEST,
                HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                429,
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR}) {
            assertEquals(ApiCacheControlHeaderWriter.NO_STORE,
                    cacheControl("GET", "/api/v1/portal/eventos/s2.abc/itens", status),
                    "status " + status + " não pode ir para a borda");
        }
    }

    @Test
    void aplicaNoStoreNoRestoDaApi() {
        assertEquals(ApiCacheControlHeaderWriter.NO_STORE, cacheControl("GET", "/api/v1/itens", 200));
        assertEquals(ApiCacheControlHeaderWriter.NO_STORE, cacheControl("GET", "/api/v1/portal/eventos/s2.x/meus-claims", 200));
        assertEquals(ApiCacheControlHeaderWriter.NO_STORE, cacheControl("GET", "/api/v1/portal/devolucoes/tok", 200));
        assertEquals(ApiCacheControlHeaderWriter.NO_STORE, cacheControl("POST", "/api/v1/portal/eventos/s2.x/claims", 201));
        assertEquals(ApiCacheControlHeaderWriter.NO_STORE, cacheControl("GET", "/swagger-ui.html", 200));
    }

    @Test
    void noStoreLevaPragmaEExpires() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);
        writer.writeHeaders(req("GET", "/api/v1/itens"), response);
        assertEquals("no-cache", response.getHeader(HttpHeaders.PRAGMA));
        assertEquals(0L, response.getDateHeader(HttpHeaders.EXPIRES));
    }

    /** Ambiente sem CDN (DEV) ou alavanca de emergência: volta tudo a no-store. */
    @Test
    void desligadoVoltaTudoParaNoStore() {
        ApiCacheControlHeaderWriter desligado = new ApiCacheControlHeaderWriter(false);
        for (String path : new String[]{
                "/api/v1/portal/cores",
                "/api/v1/portal/categorias",
                "/api/v1/portal/eventos",
                "/api/v1/portal/eventos/s2.x/itens"}) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            response.setStatus(200);
            desligado.writeHeaders(req("GET", path), response);
            assertEquals(ApiCacheControlHeaderWriter.NO_STORE, response.getHeader(HttpHeaders.CACHE_CONTROL), path);
            assertEquals("no-cache", response.getHeader(HttpHeaders.PRAGMA), path);
        }
    }

    /** Mesmo desligado, o TTL que o controller define nos binários é preservado. */
    @Test
    void desligadoAindaPreservaPoliticaDoController() {
        ApiCacheControlHeaderWriter desligado = new ApiCacheControlHeaderWriter(false);
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "public, max-age=86400");
        desligado.writeHeaders(req("GET", "/api/v1/portal/arquivos/s2.x/thumbnail"), response);
        assertEquals("public, max-age=86400", response.getHeader(HttpHeaders.CACHE_CONTROL));
    }

    @Test
    void publicoNaoLevaPragma() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);
        writer.writeHeaders(req("GET", "/api/v1/portal/cores"), response);
        assertNull(response.getHeader(HttpHeaders.PRAGMA));
        assertTrue(response.getDateHeader(HttpHeaders.EXPIRES) > System.currentTimeMillis());
    }
}
