package br.com.achadosperdidos.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ModulosConfigRequest(
        @NotNull @Valid List<ModuloToggleRequest> modulos
) {
    public record ModuloToggleRequest(
            @NotBlank String path,
            @NotNull Boolean habilitado
    ) {}
}
