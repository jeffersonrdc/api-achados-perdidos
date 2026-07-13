package br.com.achadosperdidos.controller.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ClaimResponse(
        String id, String nmNome, String nmObjeto, String nmMarca, String nmModelo, String nmCor,
        LocalDate dtPerdeu, String nmStatus, String nmCategoria, String nmEvento, LocalDateTime dtCadastro,
        String nrCpf, String nmEmail, String nrTelefone, String nmLocal, String dsObjeto
) {}
