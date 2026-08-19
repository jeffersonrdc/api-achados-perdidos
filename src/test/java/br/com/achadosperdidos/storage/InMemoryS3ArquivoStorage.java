package br.com.achadosperdidos.storage;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.concurrent.ConcurrentHashMap;

/** Backend S3 em memória para a suíte de testes (sem AWS e sem disco local). */
final class InMemoryS3ArquivoStorage implements ArquivoStorage {

    private final ConcurrentHashMap<String, byte[]> objects = new ConcurrentHashMap<>();

    @Override
    public ArquivoStorageProvider provider() {
        return ArquivoStorageProvider.S3;
    }

    private static String objectKey(String key) {
        return LocalArquivoStorage.validar(key);
    }

    @Override
    public void store(String key, InputStream content, long contentLength, String contentType) {
        try {
            objects.put(objectKey(key), content.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler stream para upload S3 em memória.", e);
        }
    }

    @Override
    public Resource open(String key) {
        byte[] data = objects.get(objectKey(key));
        if (data == null) {
            throw new IllegalArgumentException("Conteúdo do arquivo não encontrado no S3.");
        }
        return new ByteArrayResource(data);
    }

    @Override
    public boolean exists(String key) {
        return objects.containsKey(objectKey(key));
    }

    @Override
    public void delete(String key) {
        objects.remove(objectKey(key));
    }

    @Override
    public void testConnection() {
        // Sempre acessível na suíte de testes.
    }
}
