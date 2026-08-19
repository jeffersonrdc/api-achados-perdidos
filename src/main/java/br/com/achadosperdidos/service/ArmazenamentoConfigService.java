package br.com.achadosperdidos.service;

import br.com.achadosperdidos.config.S3Properties;
import br.com.achadosperdidos.controller.dto.ArmazenamentoConfigRequest;
import br.com.achadosperdidos.controller.dto.ArmazenamentoConfigResponse;
import br.com.achadosperdidos.controller.dto.ArmazenamentoTesteResponse;
import br.com.achadosperdidos.storage.ArquivoStorage;
import br.com.achadosperdidos.storage.ArquivoStorageProvider;
import br.com.achadosperdidos.storage.ArquivoStorageRouter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArmazenamentoConfigService {

    private final SistemaParametroService parametros;
    private final ArquivoStorageRouter router;
    private final S3Properties s3Properties;

    public ArmazenamentoConfigService(SistemaParametroService parametros, ArquivoStorageRouter router,
                                      S3Properties s3Properties) {
        this.parametros = parametros;
        this.router = router;
        this.s3Properties = s3Properties;
    }

    @Transactional(readOnly = true)
    public ArmazenamentoConfigResponse obter() {
        String aviso;
        if (!router.s3Disponivel()) {
            aviso = "Upload e download usam somente S3, mas o AWS SDK não está carregado.";
        } else if (!s3Properties.isConfigured()) {
            aviso = "Upload e download usam somente S3. Configure APP_S3_BUCKET e as credenciais AWS.";
        } else {
            aviso = "Upload e download usam somente o Amazon S3. Disco local do servidor não é mais utilizado.";
        }
        return new ArmazenamentoConfigResponse(
                ArquivoStorageProvider.S3.name(),
                s3Properties.isConfigured() && router.s3Disponivel(),
                blankToNull(s3Properties.getBucket()),
                blankToNull(s3Properties.getRegion()),
                blankToNull(s3Properties.prefixNormalizado()),
                s3Properties.getEndpoint() != null && !s3Properties.getEndpoint().isBlank(),
                null,
                aviso);
    }

    @Transactional
    public ArmazenamentoConfigResponse salvar(ArmazenamentoConfigRequest request) {
        ArquivoStorageProvider provider = ArquivoStorageProvider.from(request.provider());
        if (provider != ArquivoStorageProvider.S3) {
            throw new IllegalArgumentException("O armazenamento em disco local foi desativado. Use somente S3.");
        }
        if (!router.s3Disponivel()) {
            throw new IllegalArgumentException(
                    "AWS SDK não está no classpath. Faça Reload Maven no projeto api e reinicie.");
        }
        if (!s3Properties.isConfigured()) {
            throw new IllegalArgumentException(
                    "Não é possível usar S3: configure APP_S3_BUCKET e as credenciais AWS no ambiente.");
        }
        parametros.set(
                SistemaParametroService.ARQUIVO_STORAGE_PROVIDER,
                "S3",
                "Provedor de arquivos: somente S3");
        return obter();
    }

    @Transactional(readOnly = true)
    public ArmazenamentoTesteResponse testar(String providerOpcional) {
        ArquivoStorageProvider provider = providerOpcional == null || providerOpcional.isBlank()
                ? ArquivoStorageProvider.S3
                : ArquivoStorageProvider.from(providerOpcional);
        if (provider != ArquivoStorageProvider.S3) {
            return new ArmazenamentoTesteResponse(false,
                    "Teste de disco local não está disponível. Use S3.", provider.name());
        }
        try {
            ArquivoStorage storage = router.resolve(ArquivoStorageProvider.S3);
            storage.testConnection();
            return new ArmazenamentoTesteResponse(true,
                    "Conexão com S3 validada com sucesso.", "S3");
        } catch (RuntimeException e) {
            return new ArmazenamentoTesteResponse(false,
                    e.getMessage() != null ? e.getMessage() : "Falha no teste de armazenamento.", "S3");
        }
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v;
    }
}
