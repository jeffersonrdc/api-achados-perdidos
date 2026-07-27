package br.com.achadosperdidos.controller.dto;

import java.time.LocalDateTime;

public record DevolucaoHistoricoItemResponse(
        String id,
        String eventType,
        String title,
        String description,
        String actorType,
        String actorName,
        LocalDateTime createdAt
) {}
