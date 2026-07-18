package br.com.achadosperdidos.service;

import br.com.achadosperdidos.config.S3Properties;
import br.com.achadosperdidos.controller.dto.ArmazenamentoConfigRequest;
import br.com.achadosperdidos.controller.dto.ArmazenamentoConfigResponse;
import br.com.achadosperdidos.controller.dto.ArmazenamentoTesteResponse;
import br.com.achadosperdidos.storage.ArquivoStorage;
import br.com.achadosperdidos.storage.ArquivoStorageProvider;
import br.com.achadosperdidos.storage.ArquivoStorageRouter;
import br.com.achadosperdidos.storage.LocalArquivoStorage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArmazenamentoConfigService {

    private final SistemaParametroService parametros;
    private final ArquivoStorageRouter router;
    private final S3Properties s3Properties;
    private final LocalArquivoStorage localStorage;

    public ArmazenamentoConfigService(SistemaParametroService parametros, ArquivoStorageRouter router,
                                      S3Properties s3Properties, LocalArquivoStorage localStorage) {
        this.parametros = parametros;
        this.router = router;
        this.s3Properties = s3Properties;
        this.localStorage = localStorage;
    }

    @Transactional(readOnly = true)
    public ArmazenamentoConfigResponse obter() {
        ArquivoStorageProvider provider = router.provedorPadrao();
        String aviso;
        if (provider == ArquivoStorageProvider.S3 && !router.s3Disponivel()) {
            aviso = "Provedor S3 selecionado, mas o AWS SDK não está carregado. Atualize o Maven (Reload) e reinicie a API.";
        } else if (provider == ArquivoStorageProvider.S3 && !s3Properties.isConfigured()) {
            aviso = "Provedor S3 selecionado, mas APP_S3_BUCKET (e credenciais AWS) ainda não estão configurados no ambiente.";
        } else {
            aviso = "A troca do provedor afeta apenas novos uploads. Arquivos existentes continuam legíveis no provedor original.";
        }
        return new ArmazenamentoConfigResponse(
                provider.name(),
                s3Properties.isConfigured() && router.s3Disponivel(),
                blankToNull(s3Properties.getBucket()),
                blankToNull(s3Properties.getRegion()),
                blankToNull(s3Properties.getPrefix()),
                s3Properties.getEndpoint() != null && !s3Properties.getEndpoint().isBlank(),
                localStorage.getBaseDir().toString(),
                aviso);
    }

    @Transactional
    public ArmazenamentoConfigResponse salvar(ArmazenamentoConfigRequest request) {
        ArquivoStorageProvider provider = ArquivoStorageProvider.from(request.provider());
        if (provider == ArquivoStorageProvider.S3) {
            if (!router.s3Disponivel()) {
                throw new IllegalArgumentException(
                        "AWS SDK não está no classpath. Faça Reload Maven no projeto api e reinicie.");
            }
            if (!s3Properties.isConfigured()) {
                throw new IllegalArgumentException(
                        "Não é possível definir S3 como padrão: configure APP_S3_BUCKET e as credenciais AWS no ambiente.");
            }
        }
        parametros.set(
                SistemaParametroService.ARQUIVO_STORAGE_PROVIDER,
                provider.name(),
                "Provedor padrão para novos uploads: LOCAL ou S3");
        return obter();
    }

    @Transactional(readOnly = true)
    public ArmazenamentoTesteResponse testar(String providerOpcional) {
        ArquivoStorageProvider provider = providerOpcional == null || providerOpcional.isBlank()
                ? router.provedorPadrao()
                : ArquivoStorageProvider.from(providerOpcional);
        try {
            ArquivoStorage storage = router.resolve(provider);
            storage.testConnection();
            return new ArmazenamentoTesteResponse(true,
                    "Conexão com " + provider.name() + " validada com sucesso.", provider.name());
        } catch (RuntimeException e) {
            return new ArmazenamentoTesteResponse(false,
                    e.getMessage() != null ? e.getMessage() : "Falha no teste de armazenamento.", provider.name());
        }
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v;
    }
}
