package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record ItemMovimentacaoCreateRequest(@NotBlank String idItem, String idLocalizacaoOrigem, @NotBlank String idLocalizacaoDestino, @NotBlank String tpMovimento, String dsMotivo) {}
