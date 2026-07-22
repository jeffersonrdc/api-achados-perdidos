package br.com.achadosperdidos.controller.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Detalhe público completo de um item do catálogo do portal. */
public record PortalItemDetalheResponse(
        String id,
        String cdItem,
        String nmTitulo,
        String dsItem,
        String dsObservacoes,
        String nmCategoria,
        String nmSubcategoria,
        String nmMarca,
        String nmModelo,
        String nmCor,
        String nmEstado,
        LocalDate dtEncontrado,
        LocalTime hrEncontrado,
        String nmLocalEncontrado,
        String nmPosto,
        String nmLocalAtual,
        String nmStatus,
        String tpPrioridade,
        Boolean fgSensivel,
        /** ID assinado da foto principal (compatibilidade). */
        String idFotoPrincipal,
        /** Todas as fotos do item (principal primeiro) — /portal/arquivos/{id}/download */
        List<String> idsFotos
) {}
