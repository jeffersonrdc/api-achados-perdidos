package br.com.achadosperdidos.controller.dto;

import java.math.BigDecimal;

/** Dados da triagem: classificacao do item + observacoes/tags/sugestao IA e localizacao inicial. */
public record TriagemSalvarRequest(
        String idCategoria,
        String idSubcategoria,
        String nmMarca,
        String nmModelo,
        String nmCor,
        String tpPrioridade,
        Boolean fgSensivel,
        String nmEstado,
        String dsTags,
        String dsObservacao,
        String idLocalizacaoInicial,
        String dsSugestaoIa,
        BigDecimal vlConfiancaIa
) {}
