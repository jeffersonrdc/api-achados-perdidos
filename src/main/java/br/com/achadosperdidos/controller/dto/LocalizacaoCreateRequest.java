package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record LocalizacaoCreateRequest(@NotBlank String idDeposito, String nmSetor, String nmCorredor, String nmEstante, String nmPrateleira, String nmCaixa, String nmPosicao) {}
