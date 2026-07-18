package br.com.achadosperdidos.storage;

import br.com.achadosperdidos.config.S3Properties;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/** Implementação S3 — registrada como bean em {@link S3StorageBeans}, não via component-scan. */
public class S3ArquivoStorage implements ArquivoStorage {

    private final S3Properties properties;
    private final S3ClientFactory clientFactory;

    public S3ArquivoStorage(S3Properties properties, S3ClientFactory clientFactory) {
        this.properties = properties;
        this.clientFactory = clientFactory;
    }

    @Override
    public ArquivoStorageProvider provider() {
        return ArquivoStorageProvider.S3;
    }

    @Override
    public void store(String key, InputStream content, long contentLength, String contentType) {
        ensureConfigured();
        String objectKey = properties.fullKey(LocalArquivoStorage.validar(key));
        PutObjectRequest.Builder req = PutObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(objectKey);
        if (contentType != null && !contentType.isBlank()) {
            req.contentType(contentType);
        }
        try (S3Client client = clientFactory.create()) {
            if (contentLength >= 0) {
                client.putObject(req.build(), RequestBody.fromInputStream(content, contentLength));
            } else {
                byte[] bytes = content.readAllBytes();
                client.putObject(req.build(), RequestBody.fromBytes(bytes));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler stream para upload S3.", e);
        } catch (S3Exception e) {
            throw new IllegalStateException("Falha ao gravar arquivo no S3: " + e.getMessage(), e);
        }
    }

    @Override
    public Resource open(String key) {
        ensureConfigured();
        String objectKey = properties.fullKey(LocalArquivoStorage.validar(key));
        try (S3Client client = clientFactory.create()) {
            ResponseBytes<GetObjectResponse> bytes = client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(objectKey)
                    .build());
            return new ByteArrayResource(bytes.asByteArray());
        } catch (NoSuchKeyException e) {
            throw new IllegalArgumentException("Conteúdo do arquivo não encontrado no S3.");
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw new IllegalArgumentException("Conteúdo do arquivo não encontrado no S3.");
            }
            throw new IllegalStateException("Falha ao ler arquivo do S3: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean exists(String key) {
        ensureConfigured();
        String objectKey = properties.fullKey(LocalArquivoStorage.validar(key));
        try (S3Client client = clientFactory.create()) {
            client.headObject(HeadObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(objectKey)
                    .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) return false;
            throw new IllegalStateException("Falha ao verificar arquivo no S3: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String key) {
        ensureConfigured();
        String objectKey = properties.fullKey(LocalArquivoStorage.validar(key));
        try (S3Client client = clientFactory.create()) {
            client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(objectKey)
                    .build());
        } catch (S3Exception e) {
            throw new IllegalStateException("Falha ao excluir arquivo no S3: " + e.getMessage(), e);
        }
    }

    @Override
    public void testConnection() {
        ensureConfigured();
        try (S3Client client = clientFactory.create()) {
            client.headBucket(HeadBucketRequest.builder().bucket(properties.getBucket()).build());
        } catch (S3Exception e) {
            throw new IllegalStateException(
                    "Falha ao acessar bucket S3 \"" + properties.getBucket() + "\": " + e.getMessage(), e);
        }
    }

    private void ensureConfigured() {
        if (!properties.isConfigured()) {
            throw new IllegalStateException(
                    "Armazenamento S3 não configurado. Defina APP_S3_BUCKET (e região/credenciais AWS).");
        }
    }
}
