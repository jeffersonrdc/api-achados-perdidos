package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ModulosConfigResponse(
        @NotNull List<ModuloConfigItemResponse> modulos
) {}
