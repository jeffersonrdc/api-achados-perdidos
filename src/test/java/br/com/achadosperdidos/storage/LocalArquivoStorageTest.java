package br.com.achadosperdidos.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalArquivoStorageTest {

    @TempDir
    Path tempDir;

    private LocalArquivoStorage storage;

    @BeforeEach
    void setUp() {
        storage = new LocalArquivoStorage(tempDir.toString());
    }

    @Test
    void gravaLeEExcluiArquivo() throws Exception {
        byte[] bytes = "conteudo-teste".getBytes(StandardCharsets.UTF_8);
        String key = "ITEM/1/abc123.txt";

        storage.store(key, new ByteArrayInputStream(bytes), bytes.length, "text/plain");
        assertTrue(storage.exists(key));

        Resource resource = storage.open(key);
        assertEquals("conteudo-teste", new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8));

        storage.delete(key);
        assertFalse(storage.exists(key));
    }

    @Test
    void rejeitaPathTraversal() {
        assertThrows(IllegalArgumentException.class,
                () -> storage.store("../etc/passwd", new ByteArrayInputStream(new byte[]{1}), 1, "text/plain"));
        assertThrows(IllegalArgumentException.class,
                () -> LocalArquivoStorage.validar("C:/Windows/system32"));
    }

    @Test
    void testConnectionCriaDiretorioGravavel() {
        assertDoesNotThrow(() -> storage.testConnection());
        assertTrue(Files.isDirectory(tempDir));
    }
}
