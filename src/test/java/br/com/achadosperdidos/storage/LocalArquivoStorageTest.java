package br.com.achadosperdidos.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalArquivoStorageTest {

    @TempDir
    Path tempDir;

    private LocalArquivoStorage storage;

    @BeforeEach
    void setUp() {
        storage = new LocalArquivoStorage(tempDir.toString());
    }

    @Test
    void recusaGravacaoEmDisco() {
        assertThrows(IllegalStateException.class,
                () -> storage.store("ITEM/1/a.txt", new ByteArrayInputStream(new byte[]{1}), 1, "text/plain"));
        assertThrows(IllegalStateException.class, () -> storage.open("ITEM/1/a.txt"));
        assertThrows(IllegalStateException.class, () -> storage.exists("ITEM/1/a.txt"));
        assertThrows(IllegalStateException.class, () -> storage.delete("ITEM/1/a.txt"));
        assertThrows(IllegalStateException.class, () -> storage.testConnection());
    }

    @Test
    void rejeitaPathTraversalNaValidacao() {
        assertThrows(IllegalArgumentException.class,
                () -> LocalArquivoStorage.validar("../etc/passwd"));
        assertThrows(IllegalArgumentException.class,
                () -> LocalArquivoStorage.validar("C:/Windows/system32"));
    }
}
