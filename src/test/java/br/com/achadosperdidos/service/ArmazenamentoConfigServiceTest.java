package br.com.achadosperdidos.service;

import br.com.achadosperdidos.config.S3Properties;
import br.com.achadosperdidos.controller.dto.ArmazenamentoConfigRequest;
import br.com.achadosperdidos.controller.dto.ArmazenamentoConfigResponse;
import br.com.achadosperdidos.storage.ArquivoStorageProvider;
import br.com.achadosperdidos.storage.ArquivoStorageRouter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArmazenamentoConfigServiceTest {

    private SistemaParametroService parametros;
    private ArquivoStorageRouter router;
    private S3Properties s3Properties;
    private ArmazenamentoConfigService service;

    @BeforeEach
    void setUp() {
        parametros = Mockito.mock(SistemaParametroService.class);
        router = Mockito.mock(ArquivoStorageRouter.class);
        s3Properties = new S3Properties();
        when(router.provedorPadrao()).thenReturn(ArquivoStorageProvider.S3);
        when(router.s3Disponivel()).thenReturn(true);
        service = new ArmazenamentoConfigService(parametros, router, s3Properties);
    }

    @Test
    void obterSempreS3() {
        ArmazenamentoConfigResponse cfg = service.obter();
        assertEquals("S3", cfg.provider());
        assertNull(cfg.diretorioLocal());
    }

    @Test
    void salvarS3SemBucketFalha() {
        assertThrows(IllegalArgumentException.class,
                () -> service.salvar(new ArmazenamentoConfigRequest("S3")));
    }

    @Test
    void salvarLocalERejeitado() {
        assertThrows(IllegalArgumentException.class,
                () -> service.salvar(new ArmazenamentoConfigRequest("LOCAL")));
    }

    @Test
    void salvarS3ComBucketPersiste() {
        s3Properties.setBucket("achados-assets");
        ArmazenamentoConfigResponse cfg = service.salvar(new ArmazenamentoConfigRequest("S3"));
        assertEquals("S3", cfg.provider());
        verify(parametros).set(eq(SistemaParametroService.ARQUIVO_STORAGE_PROVIDER), eq("S3"), anyString());
    }

    @Test
    void testarLocalInformaDesativado() {
        var r = service.testar("LOCAL");
        assertFalse(r.sucesso());
        assertEquals("LOCAL", r.provider());
    }
}
