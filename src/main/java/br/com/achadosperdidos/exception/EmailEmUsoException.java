package br.com.achadosperdidos.exception;

public class EmailEmUsoException extends RuntimeException {
    public EmailEmUsoException(String message) {
        super(message);
    }
}
