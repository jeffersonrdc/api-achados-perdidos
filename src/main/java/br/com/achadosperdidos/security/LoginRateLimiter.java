package br.com.achadosperdidos.security;

import br.com.achadosperdidos.exception.TooManyRequestsException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Proteção a força-bruta no login (A07). Duas barreiras independentes, ambas em
 * memória (Bucket4j), sem infraestrutura externa:
 * <ul>
 *   <li><b>Por IP</b>: teto de requisições por minuto — contém varredura ampla.</li>
 *   <li><b>Por conta</b>: após N tentativas sem sucesso a conta fica bloqueada por
 *       uma janela; um login bem-sucedido zera o contador.</li>
 * </ul>
 */
@Component
public class LoginRateLimiter {

    private final boolean enabled;
    private final int ipPerMinute;
    private final int accountAttempts;
    private final Duration accountWindow;

    private final ConcurrentHashMap<String, Bucket> ipBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> accountBuckets = new ConcurrentHashMap<>();

    public LoginRateLimiter(
            @Value("${app.security.login-rate-limit.enabled:true}") boolean enabled,
            @Value("${app.security.login-rate-limit.ip-per-minute:30}") int ipPerMinute,
            @Value("${app.security.login-rate-limit.account-attempts:5}") int accountAttempts,
            @Value("${app.security.login-rate-limit.account-window-minutes:15}") int accountWindowMinutes) {
        this.enabled = enabled;
        this.ipPerMinute = Math.max(1, ipPerMinute);
        this.accountAttempts = Math.max(1, accountAttempts);
        this.accountWindow = Duration.ofMinutes(Math.max(1, accountWindowMinutes));
    }

    /** Consome uma tentativa por IP. Lança 429 se o teto por minuto for excedido. */
    public void checkIp(String ip) {
        if (!enabled || ip == null || ip.isBlank()) return;
        consumir(ipBuckets.computeIfAbsent(ip, k -> newIpBucket()),
                "Muitas tentativas a partir deste endereço. Tente novamente em instantes.");
    }

    /** Consome uma tentativa por conta. Lança 429 quando a conta está bloqueada. */
    public void checkAccount(String identificador) {
        if (!enabled || identificador == null || identificador.isBlank()) return;
        consumir(accountBuckets.computeIfAbsent(normalizar(identificador), k -> newAccountBucket()),
                "Muitas tentativas de login para esta conta. Tente novamente mais tarde.");
    }

    /** Zera o contador da conta após um login bem-sucedido. */
    public void resetAccount(String identificador) {
        if (identificador == null || identificador.isBlank()) return;
        accountBuckets.remove(normalizar(identificador));
    }

    private void consumir(Bucket bucket, String mensagem) {
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            long segundos = Math.max(1, Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds());
            throw new TooManyRequestsException(mensagem, segundos);
        }
    }

    private Bucket newIpBucket() {
        Bandwidth limite = Bandwidth.builder()
                .capacity(ipPerMinute)
                .refillGreedy(ipPerMinute, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limite).build();
    }

    private Bucket newAccountBucket() {
        // Recarga "intervally": os N créditos voltam de uma vez ao fim da janela,
        // caracterizando um bloqueio temporário depois de esgotadas as tentativas.
        Bandwidth limite = Bandwidth.builder()
                .capacity(accountAttempts)
                .refillIntervally(accountAttempts, accountWindow)
                .build();
        return Bucket.builder().addLimit(limite).build();
    }

    private static String normalizar(String identificador) {
        return identificador.trim().toLowerCase(Locale.ROOT);
    }
}
