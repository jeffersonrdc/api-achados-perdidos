package br.com.achadosperdidos.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Registro de um download de wallpaper no portal público. */
public record PortalWallpaperDownloadRequest(
        @Schema(description = "ID assinado do arquivo (arte escolhida). Opcional.", example = "s2.abc123")
        String idArquivo
) {}
