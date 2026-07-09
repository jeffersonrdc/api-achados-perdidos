package br.com.achadosperdidos.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ItemResponse(
        String id, String cdItem, String nmTitulo, String dsItem, String nmMarca, String nmModelo, String nmCor,
        LocalDate dtEncontrado, BigDecimal vlEstimado, String nmStatus, String nmCategoria, String nmEvento,
        Boolean fgEntregue, Boolean fgDescartado, LocalDateTime dtCadastro
) {}
