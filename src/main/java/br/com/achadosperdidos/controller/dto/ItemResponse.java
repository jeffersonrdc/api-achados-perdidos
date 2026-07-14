package br.com.achadosperdidos.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record ItemResponse(
        String id, String cdItem, String nmTitulo, String dsItem, String dsObservacoes,
        String nmMarca, String nmModelo, String nmCor, String nmEstado,
        LocalDate dtEncontrado, LocalTime hrEncontrado, String nmLocalEncontrado, String nmPosto,
        String nmEncontradoPor, String nmOperador,
        BigDecimal vlEstimado, String nmStatus, String nmCategoria, String nmSubcategoria,
        String nmEvento, String tpPrioridade, Boolean fgSensivel,
        Boolean fgEntregue, Boolean fgDescartado, LocalDateTime dtCadastro
) {}
