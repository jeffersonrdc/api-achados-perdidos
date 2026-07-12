package br.com.achadosperdidos.exception;

/** Lancada quando uma transicao de status do item nao e permitida pelo fluxo. */
public class TransicaoInvalidaException extends RuntimeException {
    public TransicaoInvalidaException(String message) {
        super(message);
    }
}
