package br.com.achadosperdidos.controller.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/** Conteudo da etiqueta a ser impressa (secao 5 da especificacao). */
public record EtiquetaConteudoResponse(
        String idItem,
        String protocolo,
        String conteudoQr,
        String nmTitulo,
        String nmCategoria,
        String tpPrioridade,
        Boolean fgSensivel,
        LocalDate dtEncontrado,
        LocalTime hrEncontrado,
        String nmLocalEncontrado,
        String nmImpressoraSugerida
) {}
