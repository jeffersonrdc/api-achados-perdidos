package br.com.achadosperdidos.pagination;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Lista paginada")
public record ApiPage<T>(
        @Schema(description = "Itens da página") List<T> content,
        @Schema(description = "Metadados") PaginationMeta pagination
) {
    public static <T> ApiPage<T> unpaged(List<T> content) {
        return new ApiPage<>(content, null);
    }

    public static <T> ApiPage<T> paged(List<T> content, PaginationMeta meta) {
        return new ApiPage<>(List.copyOf(content), meta);
    }
}
