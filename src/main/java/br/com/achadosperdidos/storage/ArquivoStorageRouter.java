package br.com.achadosperdidos.storage;

import br.com.achadosperdidos.config.S3Properties;
import br.com.achadosperdidos.service.SistemaParametroService;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.stereotype.Component;

/**
 * Roteia gravação pelo provedor padrão configurado e leitura pelo {@code TP_Storage}
 * persistido em cada arquivo (coexistência Local/S3).
 * <p>
 * Não referencia {@code S3ArquivoStorage} tipado: assim a API sobe em LOCAL
 * mesmo se o AWS SDK não estiver no classpath do IDE.
 */
@Component
public class ArquivoStorageRouter {

    static final String S3_BEAN = "s3ArquivoStorage";

    private final LocalArquivoStorage local;
    private final BeanFactory beanFactory;
    private final SistemaParametroService parametros;
    private final S3Properties s3Properties;

    public ArquivoStorageRouter(LocalArquivoStorage local, BeanFactory beanFactory,
                                SistemaParametroService parametros, S3Properties s3Properties) {
        this.local = local;
        this.beanFactory = beanFactory;
        this.parametros = parametros;
        this.s3Properties = s3Properties;
    }

    public ArquivoStorageProvider provedorPadrao() {
        return ArquivoStorageProvider.from(parametros.get(SistemaParametroService.ARQUIVO_STORAGE_PROVIDER, "LOCAL"));
    }

    public boolean s3Disponivel() {
        return beanFactory.containsBean(S3_BEAN);
    }

    public ArquivoStorage paraEscrita() {
        ArquivoStorageProvider provider = provedorPadrao();
        if (provider == ArquivoStorageProvider.S3) {
            if (!s3Disponivel()) {
                throw new IllegalStateException(
                        "Provedor padrão é S3, mas o AWS SDK não está no classpath. Atualize as dependências Maven e reinicie.");
            }
            if (!s3Properties.isConfigured()) {
                throw new IllegalStateException(
                        "Provedor padrão é S3, mas o bucket não está configurado (APP_S3_BUCKET).");
            }
        }
        return resolve(provider);
    }

    public ArquivoStorage paraLeitura(String tpStorage) {
        ArquivoStorageProvider provider = ArquivoStorageProvider.from(
                tpStorage == null || tpStorage.isBlank() ? "LOCAL" : tpStorage);
        return resolve(provider);
    }

    public ArquivoStorage resolve(ArquivoStorageProvider provider) {
        return switch (provider) {
            case LOCAL -> local;
            case S3 -> {
                if (!s3Disponivel()) {
                    throw new IllegalStateException(
                            "Arquivo está em S3, mas o AWS SDK não está disponível no classpath.");
                }
                yield beanFactory.getBean(S3_BEAN, ArquivoStorage.class);
            }
        };
    }
}
