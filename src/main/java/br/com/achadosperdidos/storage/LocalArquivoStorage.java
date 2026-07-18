package br.com.achadosperdidos.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Component
public class LocalArquivoStorage implements ArquivoStorage {

    private final Path baseDir;

    public LocalArquivoStorage(@Value("${app.arquivos.dir}") String arquivosDir) {
        this.baseDir = Paths.get(arquivosDir).toAbsolutePath().normalize();
    }

    @Override
    public ArquivoStorageProvider provider() {
        return ArquivoStorageProvider.LOCAL;
    }

    @Override
    public void store(String key, InputStream content, long contentLength, String contentType) {
        try {
            Path destino = resolver(key);
            Files.createDirectories(destino.getParent());
            Files.copy(content, destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gravar o arquivo em disco.", e);
        }
    }

    @Override
    public Resource open(String key) {
        Path caminho = resolver(key);
        if (!Files.exists(caminho)) {
            throw new IllegalArgumentException("Conteúdo do arquivo não encontrado em disco.");
        }
        return new FileSystemResource(caminho);
    }

    @Override
    public boolean exists(String key) {
        return Files.exists(resolver(key));
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolver(key));
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao excluir arquivo local.", e);
        }
    }

    @Override
    public void testConnection() {
        try {
            Files.createDirectories(baseDir);
            if (!Files.isWritable(baseDir)) {
                throw new IllegalStateException("Diretório de arquivos sem permissão de escrita: " + baseDir);
            }
            Path probe = baseDir.resolve(".storage-probe-" + System.nanoTime());
            Files.writeString(probe, "ok");
            Files.deleteIfExists(probe);
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao validar armazenamento local: " + e.getMessage(), e);
        }
    }

    private Path resolver(String key) {
        String normalizado = validar(key);
        Path destino = baseDir.resolve(normalizado).normalize();
        if (!destino.startsWith(baseDir)) {
            throw new IllegalArgumentException("Caminho de arquivo inválido.");
        }
        return destino;
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
