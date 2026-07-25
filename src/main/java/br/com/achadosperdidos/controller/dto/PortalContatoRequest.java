package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** POST /portal/contato — formulário "Enviar mensagem". */
public record PortalContatoRequest(
        @NotBlank @Size(max = 150) String nmNome,
        @NotBlank @Email @Size(max = 150) String nmEmail,
        @Size(max = 50) String nmAssunto,
        @NotBlank @Size(max = 1000) String dsMensagem
) {}
