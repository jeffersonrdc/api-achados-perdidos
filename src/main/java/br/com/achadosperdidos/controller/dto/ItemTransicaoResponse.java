package br.com.achadosperdidos.controller.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Resultado de uma transicao de status do item. */
public record ItemTransicaoResponse(
        String idItem,
        String statusAnterior,
        String statusNovo,
        String dsObservacao,
        LocalDateTime dtHistorico,
        List<String> proximosStatusPermitidos
) {}
