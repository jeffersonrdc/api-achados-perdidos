package br.com.achadosperdidos.storage;

import br.com.achadosperdidos.config.S3Properties;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

/** Cria clientes S3 sob demanda (credenciais via DefaultCredentialsProvider / IAM). */
public class S3ClientFactory {
    private final S3Properties properties;

    public S3ClientFactory(S3Properties properties) {
        this.properties = properties;
    }

    public S3Client create() {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(properties.getRegion() == null || properties.getRegion().isBlank()
                        ? "us-east-1" : properties.getRegion().trim()))
                .credentialsProvider(DefaultCredentialsProvider.create());

        if (properties.getEndpoint() != null && !properties.getEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(properties.getEndpoint().trim()));
        }
        if (properties.isPathStyleAccess()) {
            builder.serviceConfiguration(S3Configuration.builder()
                    .pathStyleAccessEnabled(true)
                    .build());
        }
        return builder.build();
    }
}
