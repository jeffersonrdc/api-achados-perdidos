package br.com.achadosperdidos.security;

import br.com.achadosperdidos.config.PublicRateLimitProperties;
import br.com.achadosperdidos.exception.TooManyRequestsException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
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

    private final PublicRateLimitProperties props;

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public PublicRateLimiter(PublicRateLimitProperties props) {
        this.props = props;
    }

    /**
     * Consome uma requisição para o par (ação, IP). Lança 429 se o teto por minuto for excedido.
     *
     * @param acao rótulo lógico do endpoint (ex.: {@code "portal-claim"}), para isolar limites.
     *             Cada ação pode ter teto próprio via {@code app.security.public-rate-limit.acoes}.
     * @param ip   IP de origem já normalizado; nulo/vazio é ignorado.
     */
    public void check(String acao, String ip) {
        if (!props.isEnabled() || ip == null || ip.isBlank()) return;
        String chave = acao + "|" + ip;
        Bucket bucket = buckets.computeIfAbsent(chave, k -> newBucket(props.limiteDa(acao)));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            long segundos = Math.max(1, Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds());
            throw new TooManyRequestsException(
                    "Muitas requisições a partir deste endereço. Tente novamente em instantes.", segundos);
        }
    }

    private static Bucket newBucket(int porMinuto) {
        Bandwidth limite = Bandwidth.builder()
                .capacity(porMinuto)
                .refillGreedy(porMinuto, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limite).build();
    }
}
