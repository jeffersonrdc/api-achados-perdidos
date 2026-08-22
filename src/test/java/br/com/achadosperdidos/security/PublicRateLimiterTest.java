package br.com.achadosperdidos.security;

import br.com.achadosperdidos.config.PublicRateLimitProperties;
import br.com.achadosperdidos.exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PublicRateLimiterTest {

    private static PublicRateLimiter limiter(int padrao, Map<String, Integer> acoes) {
        PublicRateLimitProperties props = new PublicRateLimitProperties();
        props.setPerMinute(padrao);
        props.setAcoes(new LinkedHashMap<>(acoes));
        return new PublicRateLimiter(props);
    }

    private static int consumidasAte429(PublicRateLimiter limiter, String acao, String ip) {
        for (int i = 1; i <= 500; i++) {
            try {
                limiter.check(acao, ip);
            } catch (TooManyRequestsException e) {
                return i - 1;
            }
        }
        return 500;
    }

    @Test
    void acaoSemTetoProprioUsaOPadrao() {
        PublicRateLimiter l = limiter(12, Map.of());
        assertEquals(12, consumidasAte429(l, "portal-claim", "203.0.113.1"));
    }

    /** Uma pagina de catalogo dispara ~20 miniaturas: 12/min faria o proprio usuario tomar 429. */
    @Test
    void leituraDeImagemTemTetoProprio() {
        PublicRateLimiter l = limiter(12, Map.of(PublicRateLimitProperties.ACAO_FOTO, 120));
        assertEquals(120, consumidasAte429(l, PublicRateLimitProperties.ACAO_FOTO, "203.0.113.2"));
        // e o teto maior nao afrouxa as demais acoes
        assertEquals(12, consumidasAte429(l, "portal-contato", "203.0.113.2"));
    }

    @Test
    void baldesSaoPorIpEPorAcao() {
        PublicRateLimiter l = limiter(3, Map.of());
        assertEquals(3, consumidasAte429(l, "portal-contato", "203.0.113.3"));
        assertEquals(3, consumidasAte429(l, "portal-contato", "203.0.113.4"), "outro IP, outro balde");
        assertEquals(3, consumidasAte429(l, "portal-registro", "203.0.113.3"), "outra acao, outro balde");
    }

    @Test
    void desligadoNaoLimita() {
        PublicRateLimitProperties props = new PublicRateLimitProperties();
        props.setEnabled(false);
        props.setPerMinute(1);
        PublicRateLimiter l = new PublicRateLimiter(props);
        assertEquals(500, consumidasAte429(l, "portal-contato", "203.0.113.5"));
    }

    @Test
    void ipVazioNaoConsomeBalde() {
        PublicRateLimiter l = limiter(1, Map.of());
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 50; i++) {
                l.check("portal-contato", null);
            }
        });
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 50; i++) {
                l.check("portal-contato", "  ");
            }
        });
    }

    @Test
    void limiteNuncaEhMenorQueUm() {
        PublicRateLimitProperties props = new PublicRateLimitProperties();
        props.setPerMinute(0);
        assertEquals(1, props.limiteDa("qualquer"));
    }

    @Test
    void padraoDeFabricaProtegeCatalogo() {
        PublicRateLimitProperties props = new PublicRateLimitProperties();
        assertEquals(120, props.limiteDa(PublicRateLimitProperties.ACAO_FOTO));
        assertEquals(12, props.limiteDa("portal-claim"));
    }
}
