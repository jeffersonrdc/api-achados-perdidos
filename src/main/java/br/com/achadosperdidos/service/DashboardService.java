package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.DashboardEventoResponse;
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
}
