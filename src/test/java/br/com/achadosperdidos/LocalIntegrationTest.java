package br.com.achadosperdidos;

import br.com.achadosperdidos.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LocalIntegrationTest extends IntegrationTestBase {

    private String adminToken;
    private String idEvento;

    @BeforeEach
    void setUp() throws Exception {
        seedBaseData();
        adminToken = login("admin", "admin123");
        idEvento = criarEvento();
    }

    @Test
    void criarLocalComFgAtivoFalseEAtualizar() throws Exception {
        var create = objectMapper.createObjectNode()
                .put("idEvento", idEvento)
                .put("nmLocal", "Posto Teste Integração")
                .put("tpLocal", "COLETA")
                .put("vlLatitude", new BigDecimal("-22.976800"))
                .put("vlLongitude", new BigDecimal("-43.391200"))
                .put("nmHorario", "Seg–Dom · 10:00–22:00")
                .put("dsObservacao", "Cadastro via teste de integração")
                .put("fgAtivo", false);

        var created = mockMvc.perform(post("/api/v1/locais")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nmLocal").value("Posto Teste Integração"))
                .andExpect(jsonPath("$.tpLocal").value("COLETA"))
                .andExpect(jsonPath("$.fgAtivo").value(false))
                .andExpect(jsonPath("$.nmHorario").value("Seg–Dom · 10:00–22:00"))
                .andReturn();

        String idLocal = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        var update = objectMapper.createObjectNode()
                .put("nmLocal", "Posto Teste Atualizado")
                .put("tpLocal", "DEPOSITO")
                .put("vlLatitude", new BigDecimal("-22.980000"))
                .put("vlLongitude", new BigDecimal("-43.400000"))
                .put("nmHorario", "Sex–Dom · 12:00–20:00")
                .put("dsObservacao", "Atualizado via teste")
                .put("fgAtivo", true);

        mockMvc.perform(put("/api/v1/locais/" + idLocal)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(idLocal))
                .andExpect(jsonPath("$.nmLocal").value("Posto Teste Atualizado"))
                .andExpect(jsonPath("$.tpLocal").value("DEPOSITO"))
                .andExpect(jsonPath("$.fgAtivo").value(true))
                .andExpect(jsonPath("$.dsObservacao").value("Atualizado via teste"));

        mockMvc.perform(get("/api/v1/locais")
                        .param("idEvento", idEvento)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nmLocal").value("Posto Teste Atualizado"))
                .andExpect(jsonPath("$[0].fgAtivo").value(true));
    }

    @Test
    void criarLocalSemFgAtivoFicaAtivoPorPadrao() throws Exception {
        var create = objectMapper.createObjectNode()
                .put("idEvento", idEvento)
                .put("nmLocal", "Local Padrão Ativo")
                .put("tpLocal", "ACHADO");

        mockMvc.perform(post("/api/v1/locais")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fgAtivo").value(true));
    }

    private String criarEvento() throws Exception {
        var body = objectMapper.createObjectNode()
                .put("idEmpresa", seedData.idEmpresa())
                .put("nmEvento", "Evento Locais Teste")
                .put("dtInicio", LocalDateTime.now().minusDays(1).toString())
                .put("dtFim", LocalDateTime.now().plusDays(7).toString())
                .put("nmCidade", "Rio de Janeiro")
                .put("sgUf", "RJ");

        var result = mockMvc.perform(post("/api/v1/eventos")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
}
