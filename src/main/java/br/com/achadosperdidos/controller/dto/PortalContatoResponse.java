package br.com.achadosperdidos.controller.dto;

/** Resposta do envio do formulário de contato do portal. */
public record PortalContatoResponse(
        String protocolo,
        String mensagem
) {}
