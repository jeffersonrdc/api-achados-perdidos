package br.com.achadosperdidos;

import br.com.achadosperdidos.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FluxoPrincipalIntegrationTest extends IntegrationTestBase {

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        seedBaseData();
        adminToken = login("admin", "admin123");
    }

    @Test
    void fluxoItemClaimValidacaoDevolucao() throws Exception {
        String idEvento = criarEvento();
        String idItem = criarItem(idEvento);
        String idClaim = criarClaim(idEvento);
        registrarValidacao(idClaim, idItem);
        registrarDevolucao(idItem, idClaim);

        mockMvc.perform(get("/api/v1/itens/" + idItem)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fgEntregue").value(true));

        mockMvc.perform(get("/api/v1/devolucoes")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].idItem").value(idItem))
                .andExpect(jsonPath("$.content[0].fgConcluido").value(true));
    }

    private String criarEvento() throws Exception {
        var body = objectMapper.createObjectNode()
                .put("nmEvento", "Festival Teste 2026")
                .put("dtInicio", LocalDateTime.now().minusDays(1).toString())
                .put("dtFim", LocalDateTime.now().plusDays(7).toString())
                .put("nmCidade", "Rio de Janeiro")
                .put("sgUf", "RJ");

        var result = mockMvc.perform(post("/api/v1/eventos")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nmEvento").value("Festival Teste 2026"))
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
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
                .andExpect(jsonPath("$.nmTitulo").value("iPhone 15 Pro"))
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String criarClaim(String idEvento) throws Exception {
        var body = objectMapper.createObjectNode()
                .put("idEvento", idEvento)
                .put("idCategoria", seedData.idCategoria())
                .put("idStatus", seedData.idStatusClaimAberto())
                .put("nmNome", "João Silva")
                .put("nmEmail", "joao@teste.com")
                .put("nmObjeto", "iPhone 15 Pro")
                .put("nmMarca", "Apple")
                .put("nmCor", "Preto");

        var result = mockMvc.perform(post("/api/v1/claims")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nmObjeto").value("iPhone 15 Pro"))
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private void registrarValidacao(String idClaim, String idItem) throws Exception {
        var body = objectMapper.createObjectNode()
                .put("idClaim", idClaim)
                .put("idItem", idItem)
                .put("qtSimilaridade", 95.5)
                .put("stResultado", "APROVADO");

        mockMvc.perform(post("/api/v1/claims/validacoes")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stResultado").value("APROVADO"));
    }

    private void registrarDevolucao(String idItem, String idClaim) throws Exception {
        var body = objectMapper.createObjectNode()
                .put("idItem", idItem)
                .put("idClaim", idClaim)
                .put("tpDevolucao", "PRESENCIAL")
                .put("nmRecebedor", "João Silva")
                .put("fgAssinado", true)
                .put("fgConcluido", true);

        mockMvc.perform(post("/api/v1/devolucoes")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fgConcluido").value(true));
    }
}
