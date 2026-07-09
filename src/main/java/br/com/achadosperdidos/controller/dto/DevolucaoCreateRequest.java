package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record DevolucaoCreateRequest(@NotBlank String idItem, String idClaim, @NotBlank String tpDevolucao, @NotBlank String nmRecebedor, String nrCpf, String dsObservacao, Boolean fgAssinado, Boolean fgConcluido) {}
