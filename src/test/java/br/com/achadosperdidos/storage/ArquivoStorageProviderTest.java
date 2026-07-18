package br.com.achadosperdidos.storage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArquivoStorageProviderTest {

    @Test
    void parseiaAliases() {
        assertEquals(ArquivoStorageProvider.LOCAL, ArquivoStorageProvider.from("local"));
        assertEquals(ArquivoStorageProvider.LOCAL, ArquivoStorageProvider.from(null));
        assertEquals(ArquivoStorageProvider.S3, ArquivoStorageProvider.from("S3"));
        assertEquals(ArquivoStorageProvider.S3, ArquivoStorageProvider.from("aws_s3"));
    }

    @Test
    void rejeitaValorInvalido() {
        assertThrows(IllegalArgumentException.class, () -> ArquivoStorageProvider.from("GCS"));
    }
}
