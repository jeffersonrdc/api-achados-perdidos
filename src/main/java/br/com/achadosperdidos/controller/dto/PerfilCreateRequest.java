package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record PerfilCreateRequest(@NotBlank String nmPerfil, String dsPerfil) {}
