package br.com.achadosperdidos.controller.dto;

import java.time.LocalDateTime;

/** Contexto público mínimo do link de resposta (sem PII sensível). */
public record PortalRespostaContextResponse(
        String protocolo,
        String objeto,
        String evento,
        String pergunta,
        LocalDateTime expiresAt,
        boolean expired,
        boolean used,
        int prazoDias
) {}
