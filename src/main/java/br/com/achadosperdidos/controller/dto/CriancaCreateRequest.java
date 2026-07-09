package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record CriancaCreateRequest(@NotBlank String idEvento, @NotBlank String nmCrianca, LocalDate dtNascimento, String nrPulseira, String nrQrCode, String dsObservacao) {}
