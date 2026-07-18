package br.com.achadosperdidos.storage;

import br.com.achadosperdidos.config.S3Properties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Beans S3 — só são registrados se o AWS SDK estiver no classpath.
 * Assim a API sobe em modo LOCAL mesmo com classpath do IDE desatualizado.
 */
@Configuration
@ConditionalOnClass(name = "software.amazon.awssdk.services.s3.S3Client")
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
