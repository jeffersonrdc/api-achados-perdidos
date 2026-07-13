package br.com.achadosperdidos.controller.dto;

import java.time.LocalDateTime;

public record DevolucaoResponse(String id, String idItem, String idClaim, String cdItem, String nmItem,
                                String nmCategoria, String nmLocalEncontrado, String tpDevolucao, String nmRecebedor,
                                String tpStatus, Boolean fgAssinado, Boolean fgConcluido, LocalDateTime dtDevolucao) {}
