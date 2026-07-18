package br.com.achadosperdidos.controller.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record ClaimUpdateRequest(
        String idCategoria,
        String idSubcategoria,
        String idStatus,
        String nmNome,
        String nrCpf,
        String nmEmail,
        String nrTelefone,
        String nmObjeto,
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
        Boolean fgAtivo
) {}
