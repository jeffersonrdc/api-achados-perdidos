package br.com.achadosperdidos.controller.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record ClaimResponse(
        String id,
        String tpClaim,
        String cdClaim,
        String nmNome,
        String nmObjeto,
        String nmMarca,
        String nmModelo,
        String nmCor,
        String nmEstado,
        String dsTags,
        String tpPrioridade,
        Boolean fgSensivel,
        LocalDate dtPerdeu,
        LocalTime hrPerdeu,
        String nmStatus,
        String idCategoria,
        String nmCategoria,
        String idSubcategoria,
        String nmSubcategoria,
        String nmEvento,
        LocalDateTime dtCadastro,
        String nrCpf,
        String nmEmail,
        String nrTelefone,
        String idLocal,
        String nmLocal,
        String dsObjeto,
        /** Item vinculado ao pedido (quando o pedido é sobre um item específico do estoque). */
        String idItem,
        String cdItem,
        String dsJustificativaAprovacao,
        String dsJustificativaReprovacao,
        /** Mensagens do solicitante ainda não vistas pelo operador. */
        Long qtMensagensNaoLidas
) {}
