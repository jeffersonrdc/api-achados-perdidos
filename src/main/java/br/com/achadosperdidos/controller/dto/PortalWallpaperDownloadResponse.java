package br.com.achadosperdidos.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Retorno do registro de download: total acumulado do evento. */
public record PortalWallpaperDownloadResponse(
        @Schema(description = "Total de downloads de wallpaper do evento", example = "1284")
        long qtDownloads
) {}
