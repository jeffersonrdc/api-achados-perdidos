package br.com.achadosperdidos.controller.dto;

import java.time.LocalDateTime;

public record DevolucaoResponse(String id, String idItem, String idClaim, String tpDevolucao, String nmRecebedor, Boolean fgAssinado, Boolean fgConcluido, LocalDateTime dtDevolucao) {}
