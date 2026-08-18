package br.com.achadosperdidos.controller.dto;

/**
 * Resultado da devolução rápida (retirada presencial no evento, com baixa imediata).
 */
public record DevolucaoRapidaResponse(
        String idClaim,
        String cdClaim,
        String idDevolucao,
        String cdProtocolo,
        String idItem,
        String cdItem,
        String nmItem,
        String nmEmail,
        Boolean emailEnviado,
        String emailAviso
) {}
