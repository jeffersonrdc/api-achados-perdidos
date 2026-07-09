package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UsuarioCreateRequest(
        @NotBlank String nmUsuario,
        @NotBlank String nmLogin,
        @NotBlank String nmEmail,
        @NotBlank String senha,
        @NotBlank String nmPerfil
) {}
