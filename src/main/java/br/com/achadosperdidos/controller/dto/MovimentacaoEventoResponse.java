package br.com.achadosperdidos.controller.dto;

import java.time.LocalDateTime;

/** Movimentação/transferência de um item, com rótulos legíveis de origem/destino. */
public record MovimentacaoEventoResponse(
        String id,
        String idItem,
        String cdItem,
        String nmTitulo,
        String tpMovimento,
        String dsMotivo,
        String origem,
        String destino,
        LocalDateTime dtMovimento
) {}
