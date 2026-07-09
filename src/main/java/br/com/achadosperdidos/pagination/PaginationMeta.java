package br.com.achadosperdidos.pagination;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Metadados de paginação (page 1-based)")
public record PaginationMeta(
        @Schema(description = "Página atual (1-based)") int page,
        @Schema(description = "Itens por página") int limit,
        @Schema(description = "Total de itens") long totalItems,
        @Schema(description = "Total de páginas") int totalPages
) {}
