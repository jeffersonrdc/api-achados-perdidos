package br.com.achadosperdidos;

import br.com.achadosperdidos.entity.AuthEvent;
import br.com.achadosperdidos.repository.AuthEventRepository;
import br.com.achadosperdidos.security.LoginRateLimiter;
import br.com.achadosperdidos.service.AuthEventService;
import br.com.achadosperdidos.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Eventos de falha usam {@code REQUIRES_NEW} e ficam invisíveis à TX REPEATABLE_READ
 * do {@link IntegrationTestBase}; por isso falhas são assertadas em TX independente.
 */
@TestPropertySource(properties = {
        "app.security.login-rate-limit.enabled=true",
        "app.security.login-rate-limit.ip-per-minute=100",
        "app.security.login-rate-limit.account-attempts=3",
        "app.security.login-rate-limit.account-window-minutes=15"
})
class LogsAcessoIntegrationTest extends IntegrationTestBase {

    @Autowired
    private LoginRateLimiter loginRateLimiter;

    @Autowired
    private AuthEventRepository authEventRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate txIndependente;

    @BeforeEach
    void setUp() {
        loginRateLimiter.resetAll();
        seedBaseData();
        txIndependente = new TransactionTemplate(transactionManager);
        txIndependente.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Test
    void loginSucessoRegistraEventoEListaNaApi() throws Exception {
        String token = login("admin", "admin123");

        mockMvc.perform(get("/api/v1/logs/acessos")
                        .header("Authorization", bearer(token))
                        .param("tpEvento", AuthEventService.LOGIN_SUCESSO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tpEvento").value(AuthEventService.LOGIN_SUCESSO))
                .andExpect(jsonPath("$.content[0].tpResultado").value(AuthEventService.RESULTADO_SUCESSO))
                .andExpect(jsonPath("$.content[0].nmUsuario").value("Administrador"))
                .andExpect(jsonPath("$.content[0].nrIp").isNotEmpty());
    }

    @Test
    void senhaInvalidaRegistraFalhaSemExporDetalheExterno() throws Exception {
        var body = objectMapper.createObjectNode()
                .put("identificador", "admin")
                .put("senha", "senha-errada");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Credenciais inválidas"));

        assertThat(eventosCommitados()).anyMatch(e ->
                AuthEventService.LOGIN_CREDENCIAL_INVALIDA.equals(e.getTpEvento())
                        && AuthEventService.RESULTADO_FALHA.equals(e.getTpResultado())
                        && "SENHA_INVALIDA".equals(e.getCdMotivo())
                        && "ad***".equals(e.getDsIdentificadorMascarado()));
    }

    @Test
    void bloqueioPorContaRegistraEvento429() throws Exception {
        var body = objectMapper.createObjectNode()
                .put("identificador", "admin")
                .put("senha", "errada");

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.codigoMotivo").value("LOGIN_RATE_LIMIT_CONTA"));

        assertThat(eventosCommitados()).anyMatch(e ->
                AuthEventService.LOGIN_RATE_LIMIT_CONTA.equals(e.getTpEvento())
                        && AuthEventService.RESULTADO_BLOQUEIO.equals(e.getTpResultado()));
    }

    @Test
    void refreshELogoutRegistramEventos() throws Exception {
        var loginBody = objectMapper.createObjectNode()
                .put("identificador", "admin")
                .put("senha", "admin123");
        String loginJson = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String refreshToken = objectMapper.readTree(loginJson).get("refreshToken").asText();
        String accessToken = objectMapper.readTree(loginJson).get("accessToken").asText();

        var refreshBody = objectMapper.createObjectNode().put("refreshToken", refreshToken);
        String refreshJson = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshBody)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String novoRefresh = objectMapper.readTree(refreshJson).get("refreshToken").asText();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                objectMapper.createObjectNode().put("refreshToken", novoRefresh))))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/logs/acessos")
                        .header("Authorization", bearer(accessToken))
                        .param("tpEvento", AuthEventService.REFRESH_SUCESSO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tpEvento").value(AuthEventService.REFRESH_SUCESSO));

        mockMvc.perform(get("/api/v1/logs/acessos")
                        .header("Authorization", bearer(accessToken))
                        .param("tpEvento", AuthEventService.LOGOUT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tpEvento").value(AuthEventService.LOGOUT));
    }

    private List<AuthEvent> eventosCommitados() {
        return txIndependente.execute(status -> authEventRepository.findAll());
    }
}
