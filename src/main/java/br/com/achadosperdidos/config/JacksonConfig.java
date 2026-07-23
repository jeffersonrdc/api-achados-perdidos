package br.com.achadosperdidos.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * O Spring Boot 4 (Spring Framework 7) migrou o stack HTTP para o Jackson 3
 * (pacote tools.jackson) e deixou de auto-configurar um bean do ObjectMapper
 * legado (Jackson 2, com.fasterxml.jackson). Como o código ainda usa a API do
 * Jackson 2 (ex.: AuditoriaService), fornecemos o bean explicitamente.
 * findAndRegisterModules() registra módulos presentes no classpath (jsr310/jdk8).
 */
@Configuration(proxyBeanMethods = false)
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }
}
