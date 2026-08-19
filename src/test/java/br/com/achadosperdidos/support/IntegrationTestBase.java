package br.com.achadosperdidos.support;

import br.com.achadosperdidos.storage.InMemoryS3StorageConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.sql.DriverManager;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import({TestDataSeeder.class, InMemoryS3StorageConfig.class})
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.open-in-view=false"
})
public abstract class IntegrationTestBase {

    protected static final ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();

    private static final String JDBC_URL = System.getenv().getOrDefault(
            "TEST_DB_URL",
            "jdbc:mysql://localhost:3306/achados_perdidos_it?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC");
    private static final String JDBC_USER = System.getenv().getOrDefault("TEST_DB_USER", "root");
    private static final String JDBC_PASS = System.getenv().getOrDefault("TEST_DB_PASS", "");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> JDBC_URL);
        registry.add("spring.datasource.username", () -> JDBC_USER);
        registry.add("spring.datasource.password", () -> JDBC_PASS);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
    }

    @BeforeAll
    static void requireMysql() {
        assumeTrue(canConnect(), "MySQL local indisponível para testes de integração (localhost:3306)");
    }

    private static boolean canConnect() {
        try {
            DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS).close();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected TestDataSeeder testDataSeeder;

    protected TestDataSeeder.SeedData seedData;

    protected void seedBaseData() {
        seedData = testDataSeeder.seed();
    }

    protected String login(String identificador, String senha) throws Exception {
        var body = objectMapper.createObjectNode()
                .put("identificador", identificador)
                .put("senha", senha);
        var result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }
}
