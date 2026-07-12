package br.com.achadosperdidos.controller.dto;

public record EquipeMembroResponse(
        String id,
        String idUsuario,
        String nmUsuario,
        String nmEmail
) {}
