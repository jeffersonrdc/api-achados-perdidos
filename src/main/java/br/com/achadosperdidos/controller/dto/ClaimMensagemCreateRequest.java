package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClaimMensagemCreateRequest(
        @NotBlank @Size(max = 2000) String dsMensagem
) {}
