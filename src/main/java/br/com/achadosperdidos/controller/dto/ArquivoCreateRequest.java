package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record ArquivoCreateRequest(@NotBlank String tpEntidade, @NotBlank String idEntidade, @NotBlank String tpArquivo, @NotBlank String nmArquivo, @NotBlank String nmPath, String tpMime, Boolean fgPrincipal, Long qtBytes) {}
