package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PortalParticipanteRegisterRequest(
        @NotBlank String nmUsuario,
        @NotBlank @Email String nmEmail,
        @NotBlank String senha,
        String nrTelefone
) {}
