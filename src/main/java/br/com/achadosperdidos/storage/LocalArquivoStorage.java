package br.com.achadosperdidos.storage;

import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utilitário de validação de keys. Upload/download em disco estão desativados
 * (não é bean Spring — evita injeção acidental).
 */
public class LocalArquivoStorage implements ArquivoStorage {

    private final Path baseDir;

    public LocalArquivoStorage(String arquivosDir) {
        this.baseDir = Paths.get(arquivosDir).toAbsolutePath().normalize();
    }

    @Override
    public ArquivoStorageProvider provider() {
        return ArquivoStorageProvider.LOCAL;
    }

    private static IllegalStateException desativado() {
        return new IllegalStateException("Armazenamento em disco local foi desativado. Use somente S3.");
    }

    @Override
    public void store(String key, InputStream content, long contentLength, String contentType) {
        throw desativado();
    }

    @Override
    public Resource open(String key) {
        throw desativado();
    }

    @Override
    public boolean exists(String key) {
        throw desativado();
    }

    @Override
    public void delete(String key) {
        throw desativado();
    }

    @Override
    public void testConnection() {
        throw desativado();
    }

    public static String validar(String relPath) {
        if (relPath == null || relPath.isBlank()) {
            throw new IllegalArgumentException("Caminho de arquivo não informado.");
        }
        String normalizado = relPath.replace('\\', '/').trim();
        if (normalizado.startsWith("/") || normalizado.contains("..") || normalizado.matches("^[A-Za-z]:.*")) {
            throw new IllegalArgumentException("Caminho de arquivo inválido.");
        }
        return normalizado;
    }

    public Path getBaseDir() {
        return baseDir;
    }
}
