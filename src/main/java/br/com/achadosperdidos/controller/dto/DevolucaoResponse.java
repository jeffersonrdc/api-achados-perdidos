package br.com.achadosperdidos.controller.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DevolucaoResponse(String id, String idItem, String idClaim, String cdItem, String nmItem,
                                String nmCategoria, String nmLocalEncontrado, String tpDevolucao, String nmRecebedor,
                                String tpStatus, Boolean fgAssinado, Boolean fgConcluido, LocalDateTime dtDevolucao,
                                String nrCpf, String nmEmail, String nrTelefone, LocalDate dtEncontrado,
                                String dsObservacao, String tpPrioridade, Boolean fgSensivel,
                                String protocol, String method, String tpClaim, String nextAction,
                                Boolean fgAtualizacaoOperador) {
    /** Compatibilidade com construtor legado (sem campos do novo fluxo). */
    public DevolucaoResponse(String id, String idItem, String idClaim, String cdItem, String nmItem,
                             String nmCategoria, String nmLocalEncontrado, String tpDevolucao, String nmRecebedor,
                             String tpStatus, Boolean fgAssinado, Boolean fgConcluido, LocalDateTime dtDevolucao,
                             String nrCpf, String nmEmail, String nrTelefone, LocalDate dtEncontrado,
                             String dsObservacao, String tpPrioridade, Boolean fgSensivel) {
        this(id, idItem, idClaim, cdItem, nmItem, nmCategoria, nmLocalEncontrado, tpDevolucao, nmRecebedor,
                tpStatus, fgAssinado, fgConcluido, dtDevolucao, nrCpf, nmEmail, nrTelefone, dtEncontrado,
                dsObservacao, tpPrioridade, fgSensivel, null, null, null, null, null);
    }
}
