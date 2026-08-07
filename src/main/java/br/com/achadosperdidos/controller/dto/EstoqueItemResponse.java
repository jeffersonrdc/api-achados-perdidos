package br.com.achadosperdidos.controller.dto;

import java.time.LocalDate;

/** Item armazenado no estoque, com a localização física. */
public record EstoqueItemResponse(
        String idItem,
        String cdItem,
        String nmTitulo,
        String nmCategoria,
        String nmSubcategoria,
        String nmMarca,
        String nmModelo,
        String nmCor,
        String tpPrioridade,
        Boolean fgSensivel,
        String nmEstado,
        LocalDate dtEncontrado,
        String nmLocalEncontrado,
        String nmPosto,
        String nmResponsavel,
        String nmStatus,
        String dsItem,
        String dsWallpaper,
        String dsObservacoes,
        String dsTags,
        String nmDeposito,
        String nmSetor,
        String nmCorredor,
        String nmEstante,
        String nmPrateleira,
        String nmCaixa,
        String nmPosicao
) {}
