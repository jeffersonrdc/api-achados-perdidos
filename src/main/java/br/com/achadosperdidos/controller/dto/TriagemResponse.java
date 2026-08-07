package br.com.achadosperdidos.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Detalhe da triagem (ou do item quando ainda não há registro de triagem). */
public record TriagemResponse(
        String id,
        String idItem,
        String cdItem,
        String nmTitulo,
        String nmStatusItem,
        String tpStatus,
        String idOperador,
        String nmOperador,
        String nmEstado,
        String nmCategoria,
        String nmSubcategoria,
        String nmCor,
        String nmMarca,
        String nmModelo,
        String dsItem,
        String dsWallpaper,
        String dsTags,
        String dsObservacao,
        String dsObservacoes,
        String dsSugestaoIa,
        BigDecimal vlConfiancaIa,
        String idLocalizacaoInicial,
        String nmPosto,
        String nmLocalEncontrado,
        Boolean fgSensivel,
        String tpPrioridade,
        LocalDateTime dtInicio,
        LocalDateTime dtConclusao
) {}
