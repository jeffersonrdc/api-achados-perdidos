package br.com.achadosperdidos.exception;

/** Link de resposta expirado ou já utilizado (HTTP 410). */
public class LinkExpiradoException extends RuntimeException {
    public LinkExpiradoException(String message) {
        super(message);
    }
}
