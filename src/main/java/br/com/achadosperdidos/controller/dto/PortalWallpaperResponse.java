package br.com.achadosperdidos.controller.dto;

/** Wallpaper disponível no portal público. */
public record PortalWallpaperResponse(
        String id,
        String nmArquivo,
        String urlDownload,
        String urlThumbnail
) {}
