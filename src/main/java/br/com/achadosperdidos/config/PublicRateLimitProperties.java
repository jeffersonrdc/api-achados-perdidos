package br.com.achadosperdidos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Limites do anti-abuso dos endpoints públicos do portal.
 *
 * <p>{@code perMinute} é o teto padrão por IP/ação. Ele foi dimensionado para ações
 * iniciadas pelo usuário (enviar contato, abrir claim, registrar-se), em que 12/min por IP
 * é folgado.</p>
 *
 * <p>Leitura de imagem é outra natureza: uma única página de catálogo dispara ~20 GETs de
 * miniatura. Com a CDN quente isso não chega à origem, mas em cache frio (início do evento,
 * itens recém-publicados) chega — e o mesmo usuário estouraria 12/min sozinho. Por isso
 * {@code acoes} permite um teto por ação sem afrouxar o anti-abuso das demais.</p>
 */
@ConfigurationProperties(prefix = "app.security.public-rate-limit")
public class PublicRateLimitProperties {

    /** Ação de leitura de imagem pública (download/thumbnail do portal). */
    public static final String ACAO_FOTO = "portal-foto";

    private boolean enabled = true;
    private int perMinute = 12;

    /** Teto por ação; ausente = usa {@link #getPerMinute()}. */
    private Map<String, Integer> acoes = new LinkedHashMap<>(Map.of(ACAO_FOTO, 120));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getPerMinute() {
        return perMinute;
    }

    public void setPerMinute(int perMinute) {
        this.perMinute = perMinute;
    }

    public Map<String, Integer> getAcoes() {
        return acoes;
    }

    public void setAcoes(Map<String, Integer> acoes) {
        this.acoes = acoes == null ? new LinkedHashMap<>() : acoes;
    }

    /** Teto efetivo da ação, sempre >= 1. */
    public int limiteDa(String acao) {
        Integer especifico = acao == null ? null : acoes.get(acao);
        return Math.max(1, especifico != null ? especifico : perMinute);
    }
}
