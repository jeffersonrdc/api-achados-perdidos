package br.com.achadosperdidos.controller.dto;

public record PortalClaimResultResponse(
        String idClaim,
        String idValidacao,
        String stValidacao,
        String mensagem
) {}
