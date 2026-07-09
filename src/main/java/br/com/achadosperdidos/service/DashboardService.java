package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.DashboardEventoResponse;
import br.com.achadosperdidos.controller.dto.DashboardSlaPendenteResponse;
import br.com.achadosperdidos.controller.dto.DashboardSlaResumoResponse;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class DashboardService {
    @PersistenceContext private EntityManager em;
    private final SignedResourceIdCodec idCodec;
    public DashboardService(SignedResourceIdCodec idCodec) { this.idCodec = idCodec; }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<DashboardEventoResponse> listarResumoEventos() {
        List<Object[]> rows = em.createNativeQuery(
                "SELECT ID_Evento, NM_Evento, QT_ItensTotal, QT_ItensPendentes, QT_ItensDevolvidos, QT_ClaimsTotal FROM VW_Dashboard_Evento"
        ).getResultList();
        return rows.stream().map(r -> new DashboardEventoResponse(
                idCodec.encodeEventoId(((Number) r[0]).longValue()),
                (String) r[1],
                r[2] != null ? ((Number) r[2]).longValue() : 0L,
                r[3] != null ? ((Number) r[3]).longValue() : 0L,
                r[4] != null ? ((Number) r[4]).longValue() : 0L,
                r[5] != null ? ((Number) r[5]).longValue() : 0L
        )).toList();
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<DashboardSlaPendenteResponse> listarSlaPendentes() {
        List<Object[]> rows = em.createNativeQuery(
                "SELECT ID_SlaRegistro, TP_Entidade, ID_Entidade, DT_Inicio, DT_Limite, ST_Sla, TP_Processo, QT_HorasLimite, QT_HorasAlerta, QT_HorasRestantes, NM_Evento FROM VW_Sla_Pendente"
        ).getResultList();
        return rows.stream().map(r -> new DashboardSlaPendenteResponse(
                idCodec.encodeSlaId(((Number) r[0]).longValue()),
                (String) r[1],
                idCodec.encodeEntidadeId((String) r[1], ((Number) r[2]).longValue()),
                r[3] != null ? ((java.sql.Timestamp) r[3]).toLocalDateTime() : null,
                r[4] != null ? ((java.sql.Timestamp) r[4]).toLocalDateTime() : null,
                (String) r[5],
                (String) r[6],
                r[7] != null ? ((Number) r[7]).intValue() : null,
                r[8] != null ? ((Number) r[8]).intValue() : null,
                r[9] != null ? ((Number) r[9]).longValue() : null,
                (String) r[10]
        )).toList();
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<DashboardSlaResumoResponse> listarSlaResumo() {
        List<Object[]> rows = em.createNativeQuery(
                "SELECT ID_Evento, NM_Evento, TP_Processo, QT_Total, QT_EmAndamento, QT_Alerta, QT_Estourado, QT_Concluido FROM VW_Sla_Resumo"
        ).getResultList();
        return rows.stream().map(r -> new DashboardSlaResumoResponse(
                r[0] != null ? idCodec.encodeEventoId(((Number) r[0]).longValue()) : null,
                (String) r[1],
                (String) r[2],
                r[3] != null ? ((Number) r[3]).longValue() : 0L,
                r[4] != null ? ((Number) r[4]).longValue() : 0L,
                r[5] != null ? ((Number) r[5]).longValue() : 0L,
                r[6] != null ? ((Number) r[6]).longValue() : 0L,
                r[7] != null ? ((Number) r[7]).longValue() : 0L
        )).toList();
    }
}
