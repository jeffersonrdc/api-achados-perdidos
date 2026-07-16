package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** Registro de uma transferência: move um ou mais itens de um local (origem) para outro (destino). */
public record TransferenciaCreateRequest(
        String idLocalOrigem,
        @NotBlank String idLocalDestino,
        @NotEmpty List<String> idsItens,
        String nmReceptor,
        String dsMotivo
) {}
