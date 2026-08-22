package br.com.achadosperdidos;

import br.com.achadosperdidos.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Magic link de devolução com token inexistente.
 *
 * <p>Regressao: o servico capturava a excecao de "token nao encontrado" lancada por outro bean
 * transacional. O proxy ja havia marcado a transacao como rollback-only, entao o commit
 * estourava {@code UnexpectedRollbackException} e a tela publica recebia 500 no lugar da
 * mensagem "Link invalido.".</p>
 */
class PortalDevolucaoTokenIntegrationTest extends IntegrationTestBase {

    @Test
    void tokenInexistenteRespondeLinkInvalidoENao500() throws Exception {
        mockMvc.perform(get("/api/v1/portal/devolucoes/token-que-nao-existe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("invalid"))
                .andExpect(jsonPath("$.expired").value(true))
                .andExpect(jsonPath("$.nextActionHint").value("Link inválido."))
                // dado pessoal por token nunca pode ir para a borda
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL,
                        "no-cache, no-store, max-age=0, must-revalidate"));
    }

    /** Token em branco e barrado antes do controller (400) — o que importa e nao ser 5xx. */
    @Test
    void tokenEmBrancoNaoViraErroDeServidor() throws Exception {
        mockMvc.perform(get("/api/v1/portal/devolucoes/%20"))
                .andExpect(status().is4xxClientError());
    }

    /** O tracking segue lancando 404 — ali "nao encontrado" nao e resultado normal. */
    @Test
    void trackingDeTokenInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/api/v1/portal/devolucoes/token-que-nao-existe/tracking"))
                .andExpect(status().isNotFound());
    }
}
