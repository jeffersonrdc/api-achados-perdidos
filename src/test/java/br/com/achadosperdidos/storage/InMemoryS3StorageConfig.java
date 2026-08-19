package br.com.achadosperdidos.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
@ConditionalOnProperty(name = "app.s3.in-memory", havingValue = "true")
public class InMemoryS3StorageConfig {

    @Bean(name = "s3ArquivoStorage")
    ArquivoStorage s3ArquivoStorage() {
        return new InMemoryS3ArquivoStorage();
    }
}
