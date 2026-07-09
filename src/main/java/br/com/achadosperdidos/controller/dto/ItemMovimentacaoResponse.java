package br.com.achadosperdidos.controller.dto;

import java.time.LocalDateTime;

public record ItemMovimentacaoResponse(String id, String idItem, String idLocalizacaoOrigem, String idLocalizacaoDestino, String tpMovimento, String dsMotivo, LocalDateTime dtMovimento) {}
