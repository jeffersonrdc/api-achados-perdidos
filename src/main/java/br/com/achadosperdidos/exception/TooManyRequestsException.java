package br.com.achadosperdidos.exception;

/**
 * Lançada quando o limite de tentativas de login é excedido (força-bruta — A07).
 * Traduzida para HTTP 429 no {@code GlobalExceptionHandler}.
 */
public class TooManyRequestsException extends RuntimeException {

    public static final String MOTIVO_IP = "LOGIN_RATE_LIMIT_IP";
    public static final String MOTIVO_CONTA = "LOGIN_RATE_LIMIT_CONTA";

    /** Segundos sugeridos para o cliente aguardar antes de tentar de novo (Retry-After). */
    private final long retryAfterSeconds;
    private final String codigoMotivo;

    public TooManyRequestsException(String message, long retryAfterSeconds) {
        this(message, retryAfterSeconds, null);
    }

    public TooManyRequestsException(String message, long retryAfterSeconds, String codigoMotivo) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
        this.codigoMotivo = codigoMotivo;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public String getCodigoMotivo() {
        return codigoMotivo;
    }
}
