package br.com.achadosperdidos.security;

import br.com.achadosperdidos.exception.TooManyRequestsException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Proteção anti-abuso dos endpoints <b>públicos</b> do portal (OWASP A04/A07): claims,
 * cadastro de crianças e registro de participante são abertos (sem login) e, sem limite,
 * ficam sujeitos a spam, criação massiva de contas e <i>harvesting</i>.
 *
 * <p>Barreira por IP + ação, em memória (Bucket4j), sem infraestrutura externa. Ao escalar
 * horizontalmente, migrar o store para Redis para um limite global entre réplicas.</p>
 */
@Component
public class PublicRateLimiter {

    private final boolean enabled;
    private final int perMinute;

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public PublicRateLimiter(
            @Value("${app.security.public-rate-limit.enabled:true}") boolean enabled,
            @Value("${app.security.public-rate-limit.per-minute:12}") int perMinute) {
        this.enabled = enabled;
        this.perMinute = Math.max(1, perMinute);
    }

    /**
     * Consome uma requisição para o par (ação, IP). Lança 429 se o teto por minuto for excedido.
     *
     * @param acao rótulo lógico do endpoint (ex.: {@code "portal-claim"}), para isolar limites.
     * @param ip   IP de origem já normalizado; nulo/vazio é ignorado.
     */
    public void check(String acao, String ip) {
        if (!enabled || ip == null || ip.isBlank()) return;
        String chave = acao + "|" + ip;
        Bucket bucket = buckets.computeIfAbsent(chave, k -> newBucket());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            long segundos = Math.max(1, Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds());
            throw new TooManyRequestsException(
                    "Muitas requisições a partir deste endereço. Tente novamente em instantes.", segundos);
        }
    }

    private Bucket newBucket() {
        Bandwidth limite = Bandwidth.builder()
                .capacity(perMinute)
                .refillGreedy(perMinute, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limite).build();
    }
}
