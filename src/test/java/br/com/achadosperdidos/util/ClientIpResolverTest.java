package br.com.achadosperdidos.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

class ClientIpResolverTest {

    private static final String VIEWER = "CloudFront-Viewer-Address";

    private static MockHttpServletRequest request(String remoteAddr, String header, String valor) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr(remoteAddr);
        if (header != null) {
            req.addHeader(header, valor);
        }
        return req;
    }

    @Test
    void semHeaderConfiguradoUsaRemoteAddr() {
        ClientIpResolver resolver = new ClientIpResolver("");
        assertEquals("10.0.0.9",
                resolver.resolve(request("10.0.0.9", VIEWER, "203.0.113.10:52432")));
    }

    @Test
    void headerConfiguradoTemPrecedenciaESemPorta() {
        ClientIpResolver resolver = new ClientIpResolver(VIEWER);
        assertEquals("203.0.113.10",
                resolver.resolve(request("10.0.0.9", VIEWER, "203.0.113.10:52432")));
    }

    @Test
    void ipv6ComPortaPerdeApenasAPorta() {
        ClientIpResolver resolver = new ClientIpResolver(VIEWER);
        assertEquals("2001:db8::1",
                resolver.resolve(request("10.0.0.9", VIEWER, "2001:db8::1:52432")));
        assertEquals("2001:db8::1",
                resolver.resolve(request("10.0.0.9", VIEWER, "[2001:db8::1]:52432")));
    }

    @Test
    void listaSeparadaPorVirgulaUsaOPrimeiro() {
        ClientIpResolver resolver = new ClientIpResolver("X-Forwarded-For");
        assertEquals("203.0.113.10",
                resolver.resolve(request("10.0.0.9", "X-Forwarded-For", "203.0.113.10, 130.176.1.1")));
    }

    @Test
    void headerAusenteOuVazioCaiNoRemoteAddr() {
        ClientIpResolver resolver = new ClientIpResolver(VIEWER);
        assertEquals("10.0.0.9", resolver.resolve(request("10.0.0.9", null, null)));
        assertEquals("10.0.0.9", resolver.resolve(request("10.0.0.9", VIEWER, "   ")));
    }

    @Test
    void normalizaLoopbackIpv6() {
        ClientIpResolver resolver = new ClientIpResolver("");
        assertEquals("127.0.0.1", resolver.resolve(request("::1", null, null)));
    }

    @Test
    void semRequestDevolveNulo() {
        assertNull(new ClientIpResolver(VIEWER).resolve(null));
    }

    @Test
    void stripPortPreservaIpv6SemPorta() {
        assertEquals("2001:db8::1", IpAddressUtil.stripPort("2001:db8::1"));
        assertEquals("203.0.113.10", IpAddressUtil.stripPort("203.0.113.10"));
        assertEquals("203.0.113.10", IpAddressUtil.stripPort("203.0.113.10:443"));
    }
}
