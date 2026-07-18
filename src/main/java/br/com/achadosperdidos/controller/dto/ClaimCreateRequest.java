package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.LocalTime;

public record ClaimCreateRequest(
        @NotBlank String idEvento,
        @NotBlank String idCategoria,
        String idSubcategoria,
        String idStatus,
        /** PERDA (padrão) ou RETIRADA. */
        String tpClaim,
        @NotBlank String nmNome,
        String nrCpf,
        String nmEmail,
        String nrTelefone,
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
        String nmLocal
) {}
