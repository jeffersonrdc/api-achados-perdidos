package br.com.achadosperdidos.controller.dto;

/** Canais de contato do portal (sistema_parametro). */
public record PortalContatosConfigResponse(
        String telefoneCentral,
        String whatsapp,
        String emailSuporte
) {}
