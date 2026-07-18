package br.com.achadosperdidos.controller.dto;

/** Resultado da validação de conexão e autenticação com o servidor SMTP. */
public record SmtpTesteResponse(boolean sucesso, String mensagem) {}
