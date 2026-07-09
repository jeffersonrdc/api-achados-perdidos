package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.LocalTime;

public record ClaimCreateRequest(
        @NotBlank String idEvento,
        @NotBlank String idCategoria,
        String idStatus,
        @NotBlank String nmNome,
        String nrCpf,
        String nmEmail,
        String nrTelefone,
        @NotBlank String nmObjeto,
        String dsObjeto,
        String nmMarca,
        String nmModelo,
        String nmCor,
        LocalDate dtPerdeu,
        LocalTime hrPerdeu,
        String nmLocal
) {}
