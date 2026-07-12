package br.com.achadosperdidos.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
        String dsTags,
        String dsObservacao,
        String dsSugestaoIa,
        BigDecimal vlConfiancaIa,
        String idLocalizacaoInicial,
        LocalDateTime dtInicio,
        LocalDateTime dtConclusao
) {}
