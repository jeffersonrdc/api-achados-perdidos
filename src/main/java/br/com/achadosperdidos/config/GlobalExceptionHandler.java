package br.com.achadosperdidos.config;

import br.com.achadosperdidos.exception.EmailEmUsoException;
import br.com.achadosperdidos.exception.PortalIndisponivelException;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.exception.TooManyRequestsException;
import br.com.achadosperdidos.exception.TransicaoInvalidaException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Credenciais inválidas");
        detail.setTitle("Não autorizado");
        detail.setType(URI.create("https://api.achadosperdidos.com/errors/unauthorized"));
        return detail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, errors);
        detail.setTitle("Dados inválidos");
        detail.setType(URI.create("https://api.achadosperdidos.com/errors/validation"));
        return detail;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.debug("Corpo HTTP não legível", ex);
        Throwable cause = ex.getMostSpecificCause();
        String message = cause instanceof UnrecognizedPropertyException
                ? "O JSON contém campo(s) não permitido(s)."
                : cause instanceof JsonParseException
                ? "JSON malformado ou incompleto."
                : "Não foi possível processar o corpo da requisição.";
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
        detail.setTitle("JSON inválido");
        return detail;
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ProblemDetail handleNotFound(RecursoNaoEncontradoException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        detail.setTitle("Recurso não encontrado");
        detail.setType(URI.create("https://api.achadosperdidos.com/errors/not-found"));
        return detail;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        detail.setTitle("Requisição inválida");
        return detail;
    }

    @ExceptionHandler(TransicaoInvalidaException.class)
    public ProblemDetail handleTransicaoInvalida(TransicaoInvalidaException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        detail.setTitle("Transição de status inválida");
        detail.setType(URI.create("https://api.achadosperdidos.com/errors/invalid-transition"));
        return detail;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        log.debug("Violação de integridade", ex);
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Operação conflita com dados existentes.");
        detail.setTitle("Conflito de dados");
        return detail;
    }

    @ExceptionHandler(EmailEmUsoException.class)
    public ProblemDetail handleEmailEmUso(EmailEmUsoException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        detail.setTitle("Conflito de e-mail");
        detail.setType(URI.create("https://api.achadosperdidos.com/errors/email-in-use"));
        return detail;
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ProblemDetail handleTooManyRequests(TooManyRequestsException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
        detail.setTitle("Muitas requisições");
        detail.setType(URI.create("https://api.achadosperdidos.com/errors/too-many-requests"));
        detail.setProperty("retryAfterSeconds", ex.getRetryAfterSeconds());
        return detail;
    }

    @ExceptionHandler(PortalIndisponivelException.class)
    public ProblemDetail handlePortalIndisponivel(PortalIndisponivelException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        detail.setTitle("Portal indisponível");
        detail.setType(URI.create("https://api.achadosperdidos.com/errors/portal-unavailable"));
        return detail;
    }
}
