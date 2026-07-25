package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.Size;

/** Atualização dos canais de contato do portal. */
public record PortalContatosConfigRequest(
        @Size(max = 40) String telefoneCentral,
        @Size(max = 40) String whatsapp,
        @Size(max = 150) String emailSuporte
) {}
