package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;

/** Avança o status da devolução (workflow de conferência → assinatura → baixa). */
public record DevolucaoStatusRequest(@NotBlank String tpStatus, String dsObservacao) {}
