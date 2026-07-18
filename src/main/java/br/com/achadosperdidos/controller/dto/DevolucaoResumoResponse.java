package br.com.achadosperdidos.controller.dto;

/** Cards/resumo da tela de devoluções (/devolucoes). */
public record DevolucaoResumoResponse(
        long total,
        long aguardandoRetirada,
        long emConferencia,
        long aguardandoAssinatura,
        long assinado,
        long concluido
) {}
