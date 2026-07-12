package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.ResumoOperacionalResponse;
import br.com.achadosperdidos.entity.Evento;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.repository.EventoRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Indicadores/analytics operacionais (secao 13 da especificacao). */
@Service
public class AnalyticsService {

    @PersistenceContext
    private EntityManager em;

    private final EventoRepository eventoRepository;
    private final SignedResourceIdCodec idCodec;

    public AnalyticsService(EventoRepository eventoRepository, SignedResourceIdCodec idCodec) {
        this.eventoRepository = eventoRepository;
        this.idCodec = idCodec;
    }

    @Transactional(readOnly = true)
    public ResumoOperacionalResponse resumoOperacional(String idEvento) {
        Long ev = idCodec.decodeEventoId(idEvento);
        Evento evento = eventoRepository.findById(ev)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento não encontrado."));

        Map<String, Long> porStatus = contarPorStatus(ev);
        long total = porStatus.values().stream().mapToLong(Long::longValue).sum();

        return new ResumoOperacionalResponse(
                idCodec.encodeEventoId(ev),
                evento.getNmEvento(),
                total,
                porStatus.getOrDefault("Encontrado", 0L),
                porStatus.getOrDefault("Coletado", 0L),
                porStatus.getOrDefault("Aguardando triagem", 0L),
                porStatus.getOrDefault("Em triagem", 0L),
                porStatus.getOrDefault("Em transporte para estoque", 0L),
                porStatus.getOrDefault("Em estoque", 0L),
                porStatus.getOrDefault("Com pedido de devolucao", 0L),
                porStatus.getOrDefault("Aguardando retirada", 0L),
                porStatus.getOrDefault("Devolvido", 0L),
                porStatus.getOrDefault("Finalizado", 0L),
                porStatus.getOrDefault("Descartado", 0L),
                contarDevolvidosHoje(ev),
                contarSensiveis(ev));
    }

    private Map<String, Long> contarPorStatus(Long ev) {
        List<Tuple> rows = em.createNativeQuery(
                        "SELECT s.NM_Status AS st, COUNT(*) AS qt " +
                                "FROM item i JOIN status_item s ON s.ID_Status = i.IDR_Status " +
                                "WHERE i.IDR_Evento = :ev AND i.FG_Excluido = 0 " +
                                "GROUP BY s.NM_Status", Tuple.class)
                .setParameter("ev", ev).getResultList();
        Map<String, Long> map = new HashMap<>();
        for (Tuple t : rows) {
            map.put((String) t.get("st"), ((Number) t.get("qt")).longValue());
        }
        return map;
    }

    private long contarDevolvidosHoje(Long ev) {
        Number n = (Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM devolucao d JOIN item i ON i.ID_Item = d.IDR_Item " +
                                "WHERE i.IDR_Evento = :ev AND d.FG_Concluido = 1 AND d.FG_Excluido = 0 " +
                                "AND DATE(d.DT_Devolucao) = CURDATE()")
                .setParameter("ev", ev).getSingleResult();
        return n.longValue();
    }

    private long contarSensiveis(Long ev) {
        Number n = (Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM item WHERE IDR_Evento = :ev AND FG_Excluido = 0 AND FG_Sensivel = 1")
                .setParameter("ev", ev).getSingleResult();
        return n.longValue();
    }
}
