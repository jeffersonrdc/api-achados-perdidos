package br.com.achadosperdidos.storage;

import br.com.achadosperdidos.config.S3Properties;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.stereotype.Component;

/**
 * Upload e download somente no S3. Disco local não é mais usado em runtime.
 */
@Component
public class ArquivoStorageRouter {

    static final String S3_BEAN = "s3ArquivoStorage";

    private final BeanFactory beanFactory;
    private final S3Properties s3Properties;

    public ArquivoStorageRouter(BeanFactory beanFactory, S3Properties s3Properties) {
        this.beanFactory = beanFactory;
        this.s3Properties = s3Properties;
    }

    public ArquivoStorageProvider provedorPadrao() {
        return ArquivoStorageProvider.S3;
    }

    public boolean s3Disponivel() {
        return beanFactory.containsBean(S3_BEAN);
    }

    public ArquivoStorage paraEscrita() {
        return s3OuFalhar("Provedor padrão é S3");
    }

    public ArquivoStorage paraLeitura(String tpStorage) {
        return s3OuFalhar("Leitura de arquivo");
    }

    public ArquivoStorage resolve(ArquivoStorageProvider provider) {
        if (provider != ArquivoStorageProvider.S3) {
            throw new IllegalStateException("Armazenamento em disco local foi desativado. Use somente S3.");
        }
        return s3OuFalhar("S3");
    }

    private ArquivoStorage s3OuFalhar(String contexto) {
        if (!s3Disponivel()) {
            throw new IllegalStateException(
                    contexto + ", mas o AWS SDK não está no classpath. Atualize as dependências Maven e reinicie.");
        }
        if (!s3Properties.isConfigured()) {
            throw new IllegalStateException(
                    contexto + ", mas o bucket não está configurado (APP_S3_BUCKET).");
        }
        return beanFactory.getBean(S3_BEAN, ArquivoStorage.class);
    }
}
