package br.com.achadosperdidos;

import br.com.achadosperdidos.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends IntegrationTestBase {

    @BeforeEach
    void setUp() {
        seedBaseData();
    }

    @Test
    void loginComCredenciaisValidasRetornaToken() throws Exception {
        var body = objectMapper.createObjectNode()
                .put("identificador", "admin")
                .put("senha", "admin123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tipoToken").value("Bearer"))
                .andExpect(jsonPath("$.usuario.nmLogin").value("admin"));
    }

    @Test
    void loginComSenhaInvalidaRetorna401() throws Exception {
        var body = objectMapper.createObjectNode()
                .put("identificador", "admin")
                .put("senha", "senha-errada");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void endpointProtegidoSemTokenRetorna401() throws Exception {
        mockMvc.perform(get("/api/v1/itens"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void endpointAdminComPerfilAtendenteRetorna403() throws Exception {
        String token = login("atendente", "atendente123");

        mockMvc.perform(get("/api/v1/usuarios")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
    }
}
