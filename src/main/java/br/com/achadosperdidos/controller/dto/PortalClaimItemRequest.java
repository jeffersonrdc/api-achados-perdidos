package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record PortalClaimItemRequest(
        @NotBlank String idItem,
        @NotBlank String nmNome,
        String nrCpf,
        @NotBlank String nmEmail,
        String nrTelefone,
        String nmContatoConfianca,
        String nrTelefoneConfianca,
        String dsRelacaoContatoConfianca,
        /** Descrição detalhada do item (formulário: "Descreva o item..."). */
        String dsObjeto,
        /** Detalhes que apenas o proprietário saberia. */
        String dsDetalhesOcultos,
        /** Compatibilidade: se dsObjeto vier vazio, usa este valor. */
        String dsObservacao
) {}
