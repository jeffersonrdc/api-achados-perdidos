package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.LocalTime;

public record ClaimCreateRequest(
        @NotBlank String idEvento,
        @NotBlank String idCategoria,
        String idSubcategoria,
        String idStatus,
        /** Alternativa a idStatus: nome do status (ex.: "Rascunho", "Claim Aberto"). */
        String nmStatus,
        /** PERDA (padrão) ou RETIRADA. */
        String tpClaim,
        @NotBlank String nmNome,
        String nrCpf,
        String nmEmail,
        String nrTelefone,
        String nmContatoConfianca,
        String nrTelefoneConfianca,
        String dsRelacaoContatoConfianca,
        @NotBlank String nmObjeto,
        String dsObjeto,
        String nmMarca,
        String nmModelo,
        String nmCor,
        String nmEstado,
        String dsTags,
        String tpPrioridade,
        Boolean fgSensivel,
        LocalDate dtPerdeu,
        LocalTime hrPerdeu,
        String idLocal,
        String nmLocal,
        String nmOperador,
        String dsObservacao
) {}
