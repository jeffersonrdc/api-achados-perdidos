package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.EvolucaoPontoResponse;
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

import java.time.LocalDate;
import java.util.ArrayList;
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

    /** Série temporal (últimos {@code dias} dias) de encontrados, devolvidos e solicitações. */
    @Transactional(readOnly = true)
    public List<EvolucaoPontoResponse> evolucao(String idEvento, int dias) {
        Long ev = idCodec.decodeEventoId(idEvento);
        eventoRepository.findById(ev)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento não encontrado."));
        int janela = Math.max(1, Math.min(dias, 90));
        LocalDate fim = LocalDate.now();
        LocalDate inicio = fim.minusDays(janela - 1L);

        Map<LocalDate, Long> encontrados = contarPorDia(
                "SELECT DATE(i.DT_Cadastro) d, COUNT(*) q FROM item i " +
                        "WHERE i.IDR_Evento = :ev AND i.FG_Excluido = 0 AND DATE(i.DT_Cadastro) BETWEEN :ini AND :fim " +
                        "GROUP BY DATE(i.DT_Cadastro)", ev, inicio, fim);
        Map<LocalDate, Long> devolvidos = contarPorDia(
                "SELECT DATE(d.DT_Devolucao) d, COUNT(*) q FROM devolucao d JOIN item i ON i.ID_Item = d.IDR_Item " +
                        "WHERE i.IDR_Evento = :ev AND d.FG_Concluido = 1 AND d.FG_Excluido = 0 " +
                        "AND DATE(d.DT_Devolucao) BETWEEN :ini AND :fim GROUP BY DATE(d.DT_Devolucao)", ev, inicio, fim);
        Map<LocalDate, Long> solicitacoes = contarPorDia(
                "SELECT DATE(c.DT_Cadastro) d, COUNT(*) q FROM claim c " +
                        "WHERE c.IDR_Evento = :ev AND c.FG_Excluido = 0 AND DATE(c.DT_Cadastro) BETWEEN :ini AND :fim " +
                        "GROUP BY DATE(c.DT_Cadastro)", ev, inicio, fim);

        List<EvolucaoPontoResponse> serie = new ArrayList<>();
        for (LocalDate d = inicio; !d.isAfter(fim); d = d.plusDays(1)) {
            serie.add(new EvolucaoPontoResponse(d,
                    encontrados.getOrDefault(d, 0L),
                    devolvidos.getOrDefault(d, 0L),
                    solicitacoes.getOrDefault(d, 0L)));
        }
        return serie;
    }

    private Map<LocalDate, Long> contarPorDia(String sql, Long ev, LocalDate ini, LocalDate fim) {
        @SuppressWarnings("unchecked")
        List<Tuple> rows = em.createNativeQuery(sql, Tuple.class)
                .setParameter("ev", ev)
                .setParameter("ini", ini)
                .setParameter("fim", fim)
                .getResultList();
        Map<LocalDate, Long> map = new HashMap<>();
        for (Tuple t : rows) {
            Object d = t.get("d");
            LocalDate data = (d instanceof java.sql.Date sd) ? sd.toLocalDate() : LocalDate.parse(d.toString());
            map.put(data, ((Number) t.get("q")).longValue());
        }
        return map;
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
