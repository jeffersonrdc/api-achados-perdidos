package br.com.achadosperdidos;

import br.com.achadosperdidos.support.IntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OpenApiDocumentationTest extends IntegrationTestBase {

    private static final Set<String> HTTP_METHODS =
            Set.of("get", "post", "put", "patch", "delete", "options", "head");

    @Autowired
    private RequestMappingHandlerMapping requestMappings;

    @Test
    void documentacaoDoPainelDeveEstarCompletaESeparadaDoPortal() throws Exception {
        JsonNode spec = obterSpec("01-painel-administrativo");

        assertTrue(spec.path("info").path("title").asText().contains("Achados e Perdidos"));
        assertTrue(spec.path("components").path("securitySchemes").has("bearerAuth"));
        assertTrue(spec.path("paths").has("/api/v1/itens"));
        assertFalse(spec.path("paths").has("/api/v1/portal/eventos"));
        validarOperacoes(spec, false);
    }

    @Test
    void documentacaoDoPortalDeveEstarCompletaSemBearerNasRotasPublicas() throws Exception {
        JsonNode spec = obterSpec("02-portal-publico");

        assertTrue(spec.path("paths").has("/api/v1/portal/eventos"));
        assertFalse(spec.path("paths").has("/api/v1/itens"));
        validarOperacoes(spec, true);

        JsonNode eventos = spec.path("paths").path("/api/v1/portal/eventos").path("get");
        assertTrue(eventos.has("security") && eventos.path("security").isEmpty());

        JsonNode meusClaims = spec.path("paths")
                .path("/api/v1/portal/eventos/{idEvento}/meus-claims").path("get");
        assertTrue(meusClaims.path("security").isArray() && !meusClaims.path("security").isEmpty());
    }

    private JsonNode obterSpec(String grupo) throws Exception {
        String docsMappings = requestMappings.getHandlerMethods().keySet().stream()
                .map(Object::toString)
                .filter(value -> value.contains("api-docs"))
                .sorted()
                .reduce("", (a, b) -> a + "\n" + b);
        assertTrue(docsMappings.contains("{group}"), "Mapping de grupos não registrado:" + docsMappings);
        var result = mockMvc.perform(get("/api-docs/" + grupo))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private static void validarOperacoes(JsonNode spec, boolean portal) {
        JsonNode paths = spec.path("paths");
        assertTrue(paths.isObject() && !paths.isEmpty());

        paths.properties().forEach(pathEntry ->
                pathEntry.getValue().properties().forEach(operationEntry -> {
                    if (!HTTP_METHODS.contains(operationEntry.getKey())) return;
                    JsonNode operation = operationEntry.getValue();
                    String location = operationEntry.getKey().toUpperCase() + " " + pathEntry.getKey();

                    assertFalse(operation.path("operationId").asText().isBlank(),
                            "operationId ausente em " + location);
                    assertFalse(operation.path("summary").asText().isBlank(),
                            "summary ausente em " + location);
                    assertFalse(operation.path("description").asText().isBlank(),
                            "description ausente em " + location);
                    assertNotNull(operation.path("responses").get("400"),
                            "HTTP 400 ausente em " + location);
                    assertNotNull(operation.path("responses").get("500"),
                            "HTTP 500 ausente em " + location);
                    if (portal) {
                        assertNotNull(operation.path("responses").get("429"),
                                "HTTP 429 ausente em " + location);
                    }
                }));
    }
}
