package br.com.achadosperdidos.storage;

import br.com.achadosperdidos.config.S3Properties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cliente S3 real — desligado nos testes de integração ({@code app.s3.in-memory=true}).
 */
@Configuration
@ConditionalOnClass(name = "software.amazon.awssdk.services.s3.S3Client")
@ConditionalOnProperty(name = "app.s3.in-memory", havingValue = "false", matchIfMissing = true)
class S3StorageBeans {

    @Bean
    S3ClientFactory s3ClientFactory(S3Properties properties) {
        return new S3ClientFactory(properties);
    }

    @Bean
    S3ArquivoStorage s3ArquivoStorage(S3Properties properties, S3ClientFactory clientFactory) {
        return new S3ArquivoStorage(properties, clientFactory);
    }
}
