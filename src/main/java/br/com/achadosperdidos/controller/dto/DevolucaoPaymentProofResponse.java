package br.com.achadosperdidos.controller.dto;

import java.time.LocalDateTime;

public record DevolucaoPaymentProofResponse(
        String fileId,
        String fileName,
        LocalDateTime uploadedAt,
        String status
) {}
