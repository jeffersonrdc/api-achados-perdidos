package br.com.achadosperdidos;

import br.com.achadosperdidos.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Politica de cache exigida pela CDN em producao e parametro {@code max} do thumbnail.
 * Roda pela cadeia real do Spring Security — e ali que o
 * {@code ApiCacheControlHeaderWriter} atua (no commit da resposta).
 */
class CacheControlIntegrationTest extends IntegrationTestBase {

    private static final String NO_STORE = "no-cache, no-store, max-age=0, must-revalidate";
    private static final String PUBLICO_CURTO = "public, max-age=60, stale-while-revalidate=300";
    private static final String PUBLICO_REF = "public, max-age=300, stale-while-revalidate=600";

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
    void getsPublicosDeReferenciaTemTtlLongo() throws Exception {
        for (String path : new String[]{
                "/api/v1/portal/categorias",
                "/api/v1/portal/cores",
                "/api/v1/portal/estados",
                "/api/v1/portal/marcas",
                "/api/v1/portal/modelos",
                "/api/v1/portal/eventos/" + idEvento + "/locais"}) {
            mockMvc.perform(get(path))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.CACHE_CONTROL, PUBLICO_REF))
                    .andExpect(header().doesNotExist(HttpHeaders.PRAGMA));
        }
    }

    @Test
    void getsPublicosDoCatalogoTemTtlCurto() throws Exception {
        for (String path : new String[]{
                "/api/v1/portal/eventos",
                "/api/v1/portal/eventos/" + idEvento,
                "/api/v1/portal/eventos/" + idEvento + "/itens",
                "/api/v1/portal/eventos/" + idEvento + "/wallpapers",
                "/api/v1/portal/status",
                "/api/v1/portal/metricas",
                "/api/v1/portal/contatos"}) {
            mockMvc.perform(get(path))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.CACHE_CONTROL, PUBLICO_CURTO));
        }
    }

    /** Um 404/500 cacheado na borda continuaria sendo servido depois de o pod se recuperar. */
    @Test
    void erroEmRotaPublicaNaoVaiParaABorda() throws Exception {
        mockMvc.perform(get("/api/v1/portal/eventos/s2.inexistente/itens"))
                .andExpect(status().is4xxClientError())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, NO_STORE));
    }

    @Test
    void rotaAutenticadaNuncaEhCacheavel() throws Exception {
        mockMvc.perform(get("/api/v1/itens").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, NO_STORE))
                .andExpect(header().string(HttpHeaders.PRAGMA, "no-cache"));

        mockMvc.perform(get("/api/v1/itens"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, NO_STORE));
    }

    /**
     * Regressao do bug em que o corpo grande commitava a resposta antes de o header ser
     * escrito, deixando listagem autenticada sair sem {@code no-store}.
     */
    @Test
    void respostaGrandeAutenticadaMantemNoStore() throws Exception {
        for (int i = 0; i < 60; i++) {
            criarItem(idEvento);
        }
        var result = mockMvc.perform(get("/api/v1/itens?limit=100")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, NO_STORE))
                .andReturn();
        assertTrue(result.getResponse().getContentAsByteArray().length > 8192,
                "o corpo precisa passar do buffer de 8 KB para o teste ter valor");
    }

    @Test
    void parametroMaxDoThumbnailMudaAResposta() throws Exception {
        String idArquivo = uploadFoto(idItem);

        byte[] p320 = thumbnail(idArquivo, "?max=320");
        byte[] p400 = thumbnail(idArquivo, "?max=400");
        byte[] p800 = thumbnail(idArquivo, "?max=800");
        byte[] padrao = thumbnail(idArquivo, "");

        assertArrayEquals(p400, padrao, "sem max deve equivaler ao padrao 400");
        assertFalse(Arrays.equals(p320, p400), "max=320 e max=400 nao podem ser identicos");
        assertFalse(Arrays.equals(p400, p800), "max=400 e max=800 nao podem ser identicos");
        assertTrue(p320.length < p800.length, "miniatura menor deve pesar menos");

        // Fora da faixa: ajusta para o topo da escada, sem erro (nao quebra cliente publicado).
        assertArrayEquals(p800, thumbnail(idArquivo, "?max=9999"));
        // Valor intermediario cai no degrau seguinte.
        assertArrayEquals(p320, thumbnail(idArquivo, "?max=300"));
    }

    @Test
    void fotoPublicaUsaTtlConfiguravel() throws Exception {
        String idArquivo = uploadFoto(idItem);
        concluirTriagem(idItem);

        mockMvc.perform(get("/api/v1/portal/arquivos/" + idArquivo + "/download"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "public, max-age=86400"));
        mockMvc.perform(get("/api/v1/portal/arquivos/" + idArquivo + "/thumbnail?max=320"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "public, max-age=86400"));
    }

    private byte[] thumbnail(String idArquivo, String query) throws Exception {
        return mockMvc.perform(get("/api/v1/arquivos/" + idArquivo + "/thumbnail" + query)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
    }

    private String uploadFoto(String idItem) throws Exception {
        MockMultipartFile foto = new MockMultipartFile(
                "file", "foto.jpg", MediaType.IMAGE_JPEG_VALUE, jpegDe(1200, 900));
        var result = mockMvc.perform(multipart("/api/v1/arquivos/upload")
                        .file(foto)
                        .param("tpEntidade", "ITEM")
                        .param("idEntidade", idItem)
                        .param("tpArquivo", "FOTO")
                        .param("fgPrincipal", "true")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    /** JPEG sintetico com faixas — redimensionar precisa mudar os bytes de verdade. */
    private static byte[] jpegDe(int largura, int altura) throws Exception {
        BufferedImage img = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            for (int x = 0; x < largura; x += 8) {
                g.setColor(new Color((x * 7) % 255, (x * 13) % 255, (x * 29) % 255));
                g.fillRect(x, 0, 8, altura);
            }
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", out);
        return out.toByteArray();
    }

    private String criarEvento() throws Exception {
        var body = objectMapper.createObjectNode()
                .put("nmEvento", "Festival Cache 2026")
                .put("dtInicio", LocalDateTime.now().minusDays(1).toString())
                .put("dtFim", LocalDateTime.now().plusDays(7).toString())
                .put("nmCidade", "Sao Paulo")
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
                .andExpect(status().isOk());
    }

    private String criarItem(String idEvento) throws Exception {
        var body = objectMapper.createObjectNode()
                .put("idEvento", idEvento)
                .put("idCategoria", seedData.idCategoria())
                .put("idStatus", seedData.idStatusEmEstoque())
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

    private void concluirTriagem(String idItem) throws Exception {
        mockMvc.perform(post("/api/v1/triagem/itens/" + idItem + "/concluir")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }
}
