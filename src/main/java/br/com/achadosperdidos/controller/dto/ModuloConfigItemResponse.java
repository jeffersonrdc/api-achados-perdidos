package br.com.achadosperdidos.controller.dto;

public record ModuloConfigItemResponse(
        String path,
        String label,
        boolean habilitado,
        boolean bloqueado
) {}
