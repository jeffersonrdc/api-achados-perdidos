package br.com.achadosperdidos;

import br.com.achadosperdidos.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PortalIntegrationTest extends IntegrationTestBase {

    private String adminToken;
    private String idEvento;
    private String idItem;

    @BeforeEach
    void setUp() throws Exception {
        seedBaseData();
        adminToken = login("admin", "admin123");
        idEvento = criarEvento();
        habilitarPortal(idEvento);
        idItem = criarItem(idEvento);
    }

    @Test
    void fluxoPortalCatalogoClaimERegistroParticipante() throws Exception {
        mockMvc.perform(get("/api/v1/portal/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(idEvento));

        mockMvc.perform(get("/api/v1/portal/eventos/" + idEvento + "/itens"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nmTitulo").value("iPhone 15 Pro"));

        mockMvc.perform(post("/api/v1/portal/eventos/" + idEvento + "/claims/item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(objectMapper.createObjectNode()
                                .put("idItem", idItem)
                                .put("nmNome", "Maria Souza")
                                .put("nmEmail", "maria.portal@teste.com")
                                .put("dsObservacao", "Tenho a nota fiscal"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stValidacao").value("PENDENTE"));

        mockMvc.perform(post("/api/v1/portal/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(objectMapper.createObjectNode()
                                .put("nmUsuario", "Maria Souza")
                                .put("nmEmail", "maria.portal@teste.com")
                                .put("senha", "senha123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nmPerfil").value("Participante"));

        String participanteToken = login("maria.portal@teste.com", "senha123");

        mockMvc.perform(get("/api/v1/portal/eventos/" + idEvento + "/meus-claims")
                        .header("Authorization", bearer(participanteToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nmObjeto").value("iPhone 15 Pro"));
    }

    private String criarEvento() throws Exception {
        var body = objectMapper.createObjectNode()
                .put("idEmpresa", seedData.idEmpresa())
                .put("nmEvento", "Festival Portal 2026")
                .put("dtInicio", LocalDateTime.now().minusDays(1).toString())
                .put("dtFim", LocalDateTime.now().plusDays(7).toString())
                .put("nmCidade", "São Paulo")
                .put("sgUf", "SP");

        var result = mockMvc.perform(post("/api/v1/eventos")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private void habilitarPortal(String idEvento) throws Exception {
        var body = objectMapper.createObjectNode()
                .put("fgConsultaPublica", true)
                .put("fgAceitaClaim", true);

        mockMvc.perform(put("/api/v1/eventos/" + idEvento + "/configuracao")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fgConsultaPublica").value(true));
    }

    private String criarItem(String idEvento) throws Exception {
        var body = objectMapper.createObjectNode()
                .put("idEvento", idEvento)
                .put("idCategoria", seedData.idCategoria())
                .put("idStatus", seedData.idStatusRecebido())
                .put("nmTitulo", "iPhone 15 Pro")
                .put("nmMarca", "Apple")
                .put("nmCor", "Preto")
                .put("dtEncontrado", LocalDate.now().toString());

        var result = mockMvc.perform(post("/api/v1/itens")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
}
