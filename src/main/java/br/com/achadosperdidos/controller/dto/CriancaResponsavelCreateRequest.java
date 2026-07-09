package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record CriancaResponsavelCreateRequest(@NotBlank String idCrianca, @NotBlank String nmResponsavel, String nrCpf, String nrRg, String nmEmail, String nrTelefone, String dsParentesco, Boolean fgPrincipal) {}
