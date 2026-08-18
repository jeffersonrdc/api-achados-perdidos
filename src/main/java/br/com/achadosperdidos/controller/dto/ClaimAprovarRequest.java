package br.com.achadosperdidos.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Aprovação do pedido. idItem é opcional quando o pedido já tem item vinculado. */
public record ClaimAprovarRequest(
        @JsonProperty("idItem") String idItem,
        @NotBlank @Size(max = 1000) @JsonProperty("dsJustificativa") String dsJustificativa
) {}
