package br.com.achadosperdidos.service;

import br.com.achadosperdidos.config.S3Properties;
import br.com.achadosperdidos.controller.dto.ArmazenamentoConfigRequest;
import br.com.achadosperdidos.controller.dto.ArmazenamentoConfigResponse;
import br.com.achadosperdidos.controller.dto.ArmazenamentoTesteResponse;
import br.com.achadosperdidos.storage.ArquivoStorageRouter;
import br.com.achadosperdidos.storage.LocalArquivoStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArmazenamentoConfigServiceTest {

    @TempDir
    Path tempDir;

    private SistemaParametroService parametros;
    private ArquivoStorageRouter router;
    private S3Properties s3Properties;
    private LocalArquivoStorage local;
    private ArmazenamentoConfigService service;

    @BeforeEach
    void setUp() {
        parametros = Mockito.mock(SistemaParametroService.class);
        router = Mockito.mock(ArquivoStorageRouter.class);
        s3Properties = new S3Properties();
        local = new LocalArquivoStorage(tempDir.toString());
        when(router.provedorPadrao()).thenReturn(br.com.achadosperdidos.storage.ArquivoStorageProvider.LOCAL);
        when(router.resolve(br.com.achadosperdidos.storage.ArquivoStorageProvider.LOCAL)).thenReturn(local);
        service = new ArmazenamentoConfigService(parametros, router, s3Properties, local);
    }

    @Test
    void obterRetornaLocalPorPadrao() {
        ArmazenamentoConfigResponse cfg = service.obter();
        assertEquals("LOCAL", cfg.provider());
        assertFalse(cfg.s3Configurado());
        assertNotNull(cfg.diretorioLocal());
    }

    @Test
    void salvarS3SemBucketFalha() {
        assertThrows(IllegalArgumentException.class,
                () -> service.salvar(new ArmazenamentoConfigRequest("S3")));
    }

    @Test
    void salvarLocalPersisteParametro() {
        ArmazenamentoConfigResponse cfg = service.salvar(new ArmazenamentoConfigRequest("LOCAL"));
        assertEquals("LOCAL", cfg.provider());
        verify(parametros).set(eq(SistemaParametroService.ARQUIVO_STORAGE_PROVIDER), eq("LOCAL"), anyString());
    }

    @Test
    void testarLocalComSucesso() {
        ArmazenamentoTesteResponse r = service.testar("LOCAL");
        assertTrue(r.sucesso());
        assertEquals("LOCAL", r.provider());
    }
}
