package br.com.achadosperdidos.controller.dto;

import java.util.List;

public record DevolucaoDetalheResponse(
        String id,
        String protocol,
        String idItem,
        String idClaim,
        String tpClaim,
        String method,
        String tpDevolucao,
        String tpStatus,
        String nmRecebedor,
        String nmEmail,
        String nrTelefone,
        String nrCpf,
        String cdItem,
        String nmItem,
        String nmCategoria,
        String nmLocalEncontrado,
        String dsObservacao,
        String nextAction,
        List<String> allowedActions,
        List<DevolucaoPickupOptionResponse> pickupOptions,
        DevolucaoShippingAddressResponse shippingAddress,
        DevolucaoShippingQuoteResponse shippingQuote,
        DevolucaoPaymentProofResponse paymentProof,
        DevolucaoShippingPostingResponse shippingPosting,
        List<DevolucaoHistoricoItemResponse> history,
        Boolean itemConferido,
        Boolean documentoConferido,
        Boolean fgAtualizacaoOperador
) {}
