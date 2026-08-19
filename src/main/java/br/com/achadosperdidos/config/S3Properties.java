package br.com.achadosperdidos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Credenciais e destino S3 via ambiente/IAM — nunca persistidos no banco.
 * Quando {@code bucket} estiver vazio, o provider S3 fica indisponível.
 */
@ConfigurationProperties(prefix = "app.s3")
public class S3Properties {
    /** Quando false, uploads para S3 são recusados mesmo com provider=S3. */
    private boolean enabled = true;
    private String bucket = "";
    private String region = "us-east-1";
    /** Prefixo opcional nas keys (ex.: rockinrio/). */
    private String prefix = "";
    /** Endpoint customizado (MinIO / LocalStack). Vazio = AWS padrão. */
    private String endpoint = "";
    private boolean pathStyleAccess = false;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public boolean isPathStyleAccess() { return pathStyleAccess; }
    public void setPathStyleAccess(boolean pathStyleAccess) { this.pathStyleAccess = pathStyleAccess; }

    public boolean isConfigured() {
        return enabled && bucket != null && !bucket.isBlank();
    }

    public String fullKey(String relativeKey) {
        String rel = relativeKey == null ? "" : relativeKey.replace('\\', '/').replaceAll("^/+", "");
        String p = prefixNormalizado();
        return p.isEmpty() ? rel : p + "/" + rel;
    }

    /** Prefixo vazio ou comentário acidental (ex.: {@code # opcional}) não entra na key. */
    public String prefixNormalizado() {
        if (prefix == null || prefix.isBlank()) return "";
        String p = prefix.trim();
        if (p.startsWith("#")) return "";
        return p.replaceAll("^/+", "").replaceAll("/+$", "");
    }
}
