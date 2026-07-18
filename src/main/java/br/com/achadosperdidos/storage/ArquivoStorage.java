package br.com.achadosperdidos.storage;

import org.springframework.core.io.Resource;

import java.io.InputStream;

/** Abstração do backend físico (disco local ou S3). */
public interface ArquivoStorage {

    ArquivoStorageProvider provider();

    void store(String key, InputStream content, long contentLength, String contentType);

    Resource open(String key);

    boolean exists(String key);

    void delete(String key);

    /** Valida se o backend está acessível (diretório gravável ou bucket listável). */
    void testConnection();
}
