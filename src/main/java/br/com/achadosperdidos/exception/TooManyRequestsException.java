package br.com.achadosperdidos.exception;

/**
 * Lançada quando o limite de tentativas de login é excedido (força-bruta — A07).
 * Traduzida para HTTP 429 no {@code GlobalExceptionHandler}.
 */
public class TooManyRequestsException extends RuntimeException {

    /** Segundos sugeridos para o cliente aguardar antes de tentar de novo (Retry-After). */
    private final long retryAfterSeconds;

    public TooManyRequestsException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
