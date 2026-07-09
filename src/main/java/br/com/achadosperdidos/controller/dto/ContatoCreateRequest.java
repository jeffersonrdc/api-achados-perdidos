package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public record ContatoCreateRequest(
        String idItem,
        String idClaim,
        @NotBlank String tpContato,
        @NotBlank String nmContato,
        String nrTelefone,
        String nmEmail,
        String dsResumo
) {}
