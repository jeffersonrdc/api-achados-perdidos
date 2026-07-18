package br.com.achadosperdidos.service;

import br.com.achadosperdidos.config.TimeConfig;
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
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Indicadores/analytics operacionais (secao 13 da especificacao). */
@Service
public class AnalyticsService {

    private static final Locale PT = Locale.forLanguageTag("pt-BR");

    @PersistenceContext
    private EntityManager em;

    private final EventoRepository eventoRepository;
    private final SignedResourceIdCodec idCodec;

    public AnalyticsService(EventoRepository eventoRepository, SignedResourceIdCodec idCodec) {
        this.eventoRepository = eventoRepository;
        this.idCodec = idCodec;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> painel(String idEvento, int dias) {
        Long ev = idCodec.decodeEventoId(idEvento);
        Evento evento = eventoRepository.findById(ev)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento não encontrado."));
        PainelChartHelper.Janela j = PainelChartHelper.janela(evento, dias);

        long encontrados = countPeriodo(
                "SELECT COUNT(*) FROM item i WHERE i.IDR_Evento = :ev AND i.FG_Excluido = 0 " +
                        "AND i.DT_Cadastro >= :ini AND i.DT_Cadastro < :fim", ev, j.inicio(), j.fim());
        long encontradosPrev = countPeriodo(
                "SELECT COUNT(*) FROM item i WHERE i.IDR_Evento = :ev AND i.FG_Excluido = 0 " +
                        "AND i.DT_Cadastro >= :ini AND i.DT_Cadastro < :fim", ev, j.prevInicio(), j.prevFim());

        long devolvidos = countPeriodo(
                "SELECT COUNT(*) FROM devolucao d WHERE d.IDR_Evento = :ev AND d.FG_Excluido = 0 AND d.FG_Concluido = 1 " +
                        "AND d.DT_Devolucao >= :ini AND d.DT_Devolucao < :fim", ev, j.inicio(), j.fim());
        long devolvidosPrev = countPeriodo(
                "SELECT COUNT(*) FROM devolucao d WHERE d.IDR_Evento = :ev AND d.FG_Excluido = 0 AND d.FG_Concluido = 1 " +
                        "AND d.DT_Devolucao >= :ini AND d.DT_Devolucao < :fim", ev, j.prevInicio(), j.prevFim());

        long emTriagem = countStatus(ev, List.of("Em triagem", "Aguardando triagem"));
        long emTriagemPrev = countStatusSnapshotPrev(ev, List.of("Em triagem", "Aguardando triagem"), j);
        long emEstoque = countStatus(ev, List.of("Em estoque"));
        long emEstoquePrev = countStatusSnapshotPrev(ev, List.of("Em estoque"), j);
        long aguardando = countStatus(ev, List.of("Aguardando retirada"));
        long aguardandoPrev = countStatusSnapshotPrev(ev, List.of("Aguardando retirada"), j);

        List<Map<String, Object>> kpis = List.of(
                PainelChartHelper.kpiChange("Itens encontrados", String.valueOf(encontrados),
                        PainelChartHelper.changePct(encontrados, encontradosPrev),
                        PainelChartHelper.changePositive(encontrados, encontradosPrev, false),
                        "bg-indigo-50 dark:bg-indigo-500/20", "text-indigo-500"),
                PainelChartHelper.kpiChange("Devolvidos", String.valueOf(devolvidos),
                        PainelChartHelper.changePct(devolvidos, devolvidosPrev),
                        PainelChartHelper.changePositive(devolvidos, devolvidosPrev, false),
                        "bg-emerald-50 dark:bg-emerald-500/20", "text-emerald-500"),
                PainelChartHelper.kpiChange("Em triagem", String.valueOf(emTriagem),
                        PainelChartHelper.changePct(emTriagem, emTriagemPrev),
                        PainelChartHelper.changePositive(emTriagem, emTriagemPrev, true),
                        "bg-amber-50 dark:bg-amber-500/20", "text-amber-500"),
                PainelChartHelper.kpiChange("Em estoque", String.valueOf(emEstoque),
                        PainelChartHelper.changePct(emEstoque, emEstoquePrev),
                        PainelChartHelper.changePositive(emEstoque, emEstoquePrev, true),
                        "bg-purple-50 dark:bg-purple-500/20", "text-purple-500"),
                PainelChartHelper.kpiChange("Aguardando retirada", String.valueOf(aguardando),
                        deltaItens(aguardando, aguardandoPrev),
                        PainelChartHelper.changePositive(aguardando, aguardandoPrev, true),
                        "bg-sky-50 dark:bg-sky-500/20", "text-sky-500"));

        List<Map<String, Object>> taxaCategoria = taxaPorDimensao(ev, j, DimensaoTaxa.CATEGORIA);
        List<Map<String, Object>> taxaLocal = taxaPorDimensao(ev, j, DimensaoTaxa.LOCAL);

        long encTotal = taxaCategoria.stream().mapToLong(r -> PainelChartHelper.asLong(r.get("encontrados"))).sum();
        long devTotal = taxaCategoria.stream().mapToLong(r -> PainelChartHelper.asLong(r.get("devolvidos"))).sum();
        int taxaGeral = encTotal > 0 ? (int) Math.round((devTotal * 100.0) / encTotal) : 0;

        List<Map<String, Object>> taxaPeriodo = List.of(Map.of(
                "name", "Taxa de devolução (%)",
                "series", serieTaxaDiaria(ev, j)));

        List<Map<String, Object>> tempos = temposMedios(ev, j);
        List<Map<String, Object>> incidencia = incidenciaLocais(ev, j);
        List<Map.Entry<String, Number>> topMutable = new ArrayList<>();
        for (int i = 0; i < incidencia.size() && i < 8; i++) {
            Map<String, Object> r = incidencia.get(i);
            topMutable.add(new java.util.AbstractMap.SimpleEntry<>(
                    String.valueOf(r.get("nome")), PainelChartHelper.asLong(r.get("achados"))));
        }

        List<Map.Entry<String, Number>> catEntries = new ArrayList<>();
        for (Map<String, Object> r : taxaCategoria) {
            catEntries.add(Map.entry(String.valueOf(r.get("dimensao")), PainelChartHelper.asLong(r.get("encontrados"))));
        }

        List<Map<String, Object>> horariosSeries = horariosSeries(ev, j);
        List<Map<String, Object>> slaEquipes = slaEquipes(ev, j);
        List<Map<String, Object>> itensParados = itensParados(ev);
        List<Map<String, Object>> gargalos = gargalos(slaEquipes, itensParados);
        List<Map<String, Object>> previsaoDia = previsaoDiaSeries(ev, j);
        List<Map<String, Object>> previsaoLocal = previsaoLocal(ev, j);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("dias", j.dias());
        out.put("inicio", j.inicio().toString());
        out.put("fim", j.fim().toString());
        out.put("metodoPrevisao", "Média móvel de 3 dias + tendência linear do período; confiança baseada no CV da série.");
        out.put("kpis", kpis);
        out.put("taxaGeral", taxaGeral);
        out.put("taxaCategoria", taxaCategoria);
        out.put("taxaLocal", taxaLocal);
        out.put("taxaPeriodo", taxaPeriodo);
        out.put("tempos", tempos);
        out.put("incidencia", incidencia);
        out.put("topIncidencia", PainelChartHelper.bars(topMutable));
        out.put("categorias", PainelChartHelper.slices(catEntries));
        out.put("horariosSeries", horariosSeries);
        out.put("slaEquipes", slaEquipes);
        out.put("itensParados", itensParados);
        out.put("gargalos", gargalos);
        out.put("previsaoDia", previsaoDia);
        out.put("previsaoLocal", previsaoLocal);
        return out;
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
        LocalDate fim = LocalDate.now(TimeConfig.ZONE_BRASILIA);
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

    // ------------------------------------------------------------------
    // Blocos do painel
    // ------------------------------------------------------------------

    /**
     * Dimensões permitidas para taxa de devolução — SQL completo e estático
     * (sem concatenação de fragmentos) para evitar falso positivo de SQLi no Semgrep.
     */
    private enum DimensaoTaxa {
        CATEGORIA(
                """
                SELECT COALESCE(c.NM_Categoria, 'Não informado') dimensao, COUNT(*) encontrados
                FROM item i
                LEFT JOIN categoria c ON c.ID_Categoria = i.IDR_Categoria
                WHERE i.IDR_Evento = :ev AND i.FG_Excluido = 0
                  AND i.DT_Cadastro >= :ini AND i.DT_Cadastro < :fim
                GROUP BY c.NM_Categoria
                ORDER BY encontrados DESC
                LIMIT 12
                """,
                """
                SELECT COALESCE(c.NM_Categoria, 'Não informado') dimensao, COUNT(*) devolvidos
                FROM devolucao d
                JOIN item i ON i.ID_Item = d.IDR_Item
                LEFT JOIN categoria c ON c.ID_Categoria = i.IDR_Categoria
                WHERE d.IDR_Evento = :ev AND d.FG_Excluido = 0 AND d.FG_Concluido = 1
                  AND d.DT_Devolucao >= :ini AND d.DT_Devolucao < :fim
                GROUP BY c.NM_Categoria
                """),
        LOCAL(
                """
                SELECT COALESCE(NULLIF(i.NM_LocalEncontrado, ''), l.NM_Local, 'Não informado') dimensao,
                       COUNT(*) encontrados
                FROM item i
                LEFT JOIN local l ON l.ID_Local = i.IDR_LocalAtual
                WHERE i.IDR_Evento = :ev AND i.FG_Excluido = 0
                  AND i.DT_Cadastro >= :ini AND i.DT_Cadastro < :fim
                GROUP BY COALESCE(NULLIF(i.NM_LocalEncontrado, ''), l.NM_Local, 'Não informado')
                ORDER BY encontrados DESC
                LIMIT 12
                """,
                """
                SELECT COALESCE(NULLIF(i.NM_LocalEncontrado, ''), l.NM_Local, 'Não informado') dimensao,
                       COUNT(*) devolvidos
                FROM devolucao d
                JOIN item i ON i.ID_Item = d.IDR_Item
                LEFT JOIN local l ON l.ID_Local = i.IDR_LocalAtual
                WHERE d.IDR_Evento = :ev AND d.FG_Excluido = 0 AND d.FG_Concluido = 1
                  AND d.DT_Devolucao >= :ini AND d.DT_Devolucao < :fim
                GROUP BY COALESCE(NULLIF(i.NM_LocalEncontrado, ''), l.NM_Local, 'Não informado')
                """);

        final String sqlEncontrados;
        final String sqlDevolvidos;

        DimensaoTaxa(String sqlEncontrados, String sqlDevolvidos) {
            this.sqlEncontrados = sqlEncontrados;
            this.sqlDevolvidos = sqlDevolvidos;
        }
    }

    private List<Map<String, Object>> taxaPorDimensao(Long ev, PainelChartHelper.Janela j, DimensaoTaxa dim) {
        @SuppressWarnings("unchecked")
        List<Tuple> enc = em.createNativeQuery(dim.sqlEncontrados, Tuple.class)
                .setParameter("ev", ev).setParameter("ini", j.inicio()).setParameter("fim", j.fim())
                .getResultList();

        Map<String, Long> devolvidosPorDim = new HashMap<>();
        @SuppressWarnings("unchecked")
        List<Tuple> dev = em.createNativeQuery(dim.sqlDevolvidos, Tuple.class)
                .setParameter("ev", ev).setParameter("ini", j.inicio()).setParameter("fim", j.fim())
                .getResultList();
        for (Tuple t : dev) {
            devolvidosPorDim.put(String.valueOf(t.get("dimensao")), PainelChartHelper.asLong(t.get("devolvidos")));
        }

        List<Map<String, Object>> out = new ArrayList<>();
        for (Tuple t : enc) {
            String dimensao = String.valueOf(t.get("dimensao"));
            long e = PainelChartHelper.asLong(t.get("encontrados"));
            long d = devolvidosPorDim.getOrDefault(dimensao, 0L);
            int taxa = e > 0 ? (int) Math.round((d * 100.0) / e) : 0;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("dimensao", dimensao);
            m.put("encontrados", e);
            m.put("devolvidos", d);
            m.put("taxa", taxa);
            out.add(m);
        }
        return out;
    }

    private List<Map<String, Object>> serieTaxaDiaria(Long ev, PainelChartHelper.Janela j) {
        LocalDate ini = j.inicio().toLocalDate();
        LocalDate fim = j.fim().toLocalDate();
        if (fim.isBefore(ini)) fim = ini;

        Map<LocalDate, Long> enc = contarPorDia(
                "SELECT DATE(i.DT_Cadastro) d, COUNT(*) q FROM item i WHERE i.IDR_Evento = :ev AND i.FG_Excluido = 0 " +
                        "AND DATE(i.DT_Cadastro) BETWEEN :ini AND :fim GROUP BY DATE(i.DT_Cadastro)",
                ev, ini, fim);
        Map<LocalDate, Long> dev = contarPorDia(
                "SELECT DATE(d.DT_Devolucao) d, COUNT(*) q FROM devolucao d WHERE d.IDR_Evento = :ev " +
                        "AND d.FG_Excluido = 0 AND d.FG_Concluido = 1 AND DATE(d.DT_Devolucao) BETWEEN :ini AND :fim " +
                        "GROUP BY DATE(d.DT_Devolucao)",
                ev, ini, fim);

        List<Map<String, Object>> series = new ArrayList<>();
        for (LocalDate d = ini; !d.isAfter(fim); d = d.plusDays(1)) {
            long e = enc.getOrDefault(d, 0L);
            long v = dev.getOrDefault(d, 0L);
            int taxa = e > 0 ? (int) Math.round((v * 100.0) / e) : 0;
            series.add(Map.of("name", labelDia(d), "value", taxa));
        }
        return series;
    }

    private List<Map<String, Object>> temposMedios(Long ev, PainelChartHelper.Janela j) {
        Map<String, Double> metas = metasSla(ev);
        double metaCadastro = metas.getOrDefault("CADASTRO", 0.5);
        double metaTriagem = metas.getOrDefault("TRIAGEM", 4.0);
        double metaEstoque = metas.getOrDefault("ESTOQUE", 2.0);
        double metaDevolucao = metas.getOrDefault("DEVOLUCAO", 4.0);

        double hCadastro = avgHours(
                "SELECT AVG(TIMESTAMPDIFF(MINUTE, TIMESTAMP(i.DT_Encontrado, COALESCE(i.HR_Encontrado, '00:00:00')), i.DT_Cadastro) / 60.0) " +
                        "FROM item i WHERE i.IDR_Evento = :ev AND i.FG_Excluido = 0 " +
                        "AND i.DT_Cadastro >= :ini AND i.DT_Cadastro < :fim " +
                        "AND i.DT_Encontrado IS NOT NULL", ev, j);
        double hTriagem = avgHours(
                "SELECT AVG(TIMESTAMPDIFF(MINUTE, tr.DT_Inicio, tr.DT_Conclusao) / 60.0) " +
                        "FROM triagem tr JOIN item i ON i.ID_Item = tr.IDR_Item " +
                        "WHERE i.IDR_Evento = :ev AND tr.FG_Excluido = 0 AND tr.TP_Status = 'CONCLUIDA' " +
                        "AND tr.DT_Inicio IS NOT NULL AND tr.DT_Conclusao IS NOT NULL " +
                        "AND tr.DT_Conclusao >= :ini AND tr.DT_Conclusao < :fim", ev, j);
        double hEstoque = avgHours(
                "SELECT AVG(TIMESTAMPDIFF(MINUTE, h1.DT_Historico, h2.DT_Historico) / 60.0) " +
                        "FROM item_historico h2 " +
                        "JOIN status_item s2 ON s2.ID_Status = h2.IDR_StatusNovo AND s2.NM_Status = 'Em estoque' " +
                        "JOIN item i ON i.ID_Item = h2.IDR_Item " +
                        "JOIN item_historico h1 ON h1.IDR_Item = i.ID_Item AND h1.FG_Excluido = 0 " +
                        "JOIN status_item s1 ON s1.ID_Status = h1.IDR_StatusNovo " +
                        "  AND s1.NM_Status IN ('Em transporte para estoque', 'Em triagem', 'Aguardando triagem') " +
                        "WHERE i.IDR_Evento = :ev AND h2.FG_Excluido = 0 " +
                        "AND h2.DT_Historico >= :ini AND h2.DT_Historico < :fim " +
                        "AND h1.DT_Historico = ( " +
                        "  SELECT MAX(hx.DT_Historico) FROM item_historico hx " +
                        "  JOIN status_item sx ON sx.ID_Status = hx.IDR_StatusNovo " +
                        "  WHERE hx.IDR_Item = i.ID_Item AND hx.FG_Excluido = 0 AND hx.DT_Historico <= h2.DT_Historico " +
                        "  AND sx.NM_Status IN ('Em transporte para estoque', 'Em triagem', 'Aguardando triagem'))", ev, j);
        double hDevolucao = avgHours(
                "SELECT AVG(TIMESTAMPDIFF(MINUTE, c.DT_Alteracao, d.DT_Devolucao) / 60.0) " +
                        "FROM devolucao d JOIN claim c ON c.ID_Claim = d.IDR_Claim " +
                        "JOIN status_item s ON s.ID_Status = c.IDR_Status AND s.NM_Status = 'Claim Aprovado' " +
                        "WHERE d.IDR_Evento = :ev AND d.FG_Excluido = 0 AND d.FG_Concluido = 1 " +
                        "AND d.DT_Devolucao >= :ini AND d.DT_Devolucao < :fim AND c.DT_Alteracao IS NOT NULL", ev, j);

        // Fallback: se histórico não tiver dados, usar diferença cadastro→estoque aproximada
        if (hEstoque <= 0) {
            hEstoque = avgHours(
                    "SELECT AVG(TIMESTAMPDIFF(MINUTE, i.DT_Cadastro, i.DT_Alteracao) / 60.0) " +
                            "FROM item i JOIN status_item s ON s.ID_Status = i.IDR_Status AND s.NM_Status = 'Em estoque' " +
                            "WHERE i.IDR_Evento = :ev AND i.FG_Excluido = 0 AND i.DT_Alteracao IS NOT NULL " +
                            "AND i.DT_Alteracao >= :ini AND i.DT_Alteracao < :fim", ev, j);
        }
        if (hDevolucao <= 0) {
            hDevolucao = avgHours(
                    "SELECT AVG(TIMESTAMPDIFF(MINUTE, d.DT_Cadastro, d.DT_Devolucao) / 60.0) " +
                            "FROM devolucao d WHERE d.IDR_Evento = :ev AND d.FG_Excluido = 0 AND d.FG_Concluido = 1 " +
                            "AND d.DT_Devolucao >= :ini AND d.DT_Devolucao < :fim", ev, j);
        }

        return List.of(
                tempoEtapa("Cadastro (coleta → sistema)", hCadastro, metaCadastro),
                tempoEtapa("Triagem", hTriagem, metaTriagem),
                tempoEtapa("Estoque (entrada → prateleira)", hEstoque, metaEstoque),
                tempoEtapa("Devolução (aprovação → entrega)", hDevolucao, metaDevolucao));
    }

    private Map<String, Object> tempoEtapa(String etapa, double horas, double meta) {
        double h = Math.max(0, PainelChartHelper.round1(horas));
        String status;
        if (h <= meta) status = "Dentro do SLA";
        else if (h <= meta * 1.25) status = "Atenção";
        else status = "Crítico";
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("etapa", etapa);
        m.put("horas", h);
        m.put("meta", meta);
        m.put("status", status);
        return m;
    }

    private Map<String, Double> metasSla(Long ev) {
        @SuppressWarnings("unchecked")
        List<Tuple> rows = em.createNativeQuery(
                        "SELECT TP_Processo tp, QT_HorasLimite h FROM sla_regra " +
                                "WHERE FG_Excluido = 0 AND FG_Ativo = 1 AND (IDR_Evento = :ev OR IDR_Evento IS NULL)", Tuple.class)
                .setParameter("ev", ev).getResultList();
        Map<String, Double> map = new HashMap<>();
        for (Tuple t : rows) {
            map.put(String.valueOf(t.get("tp")).toUpperCase(Locale.ROOT), PainelChartHelper.asDouble(t.get("h")));
        }
        return map;
    }

    private List<Map<String, Object>> incidenciaLocais(Long ev, PainelChartHelper.Janela j) {
        @SuppressWarnings("unchecked")
        List<Tuple> rows = em.createNativeQuery(
                        "SELECT l.NM_Local nome, l.VL_Latitude lat, l.VL_Longitude lng, " +
                                "COALESCE(cnt.q, 0) achados " +
                                "FROM local l " +
                                "LEFT JOIN ( " +
                                "  SELECT COALESCE(i.IDR_LocalAtual, loc.ID_Local) idLocal, COUNT(*) q " +
                                "  FROM item i " +
                                "  LEFT JOIN local loc ON loc.IDR_Evento = i.IDR_Evento " +
                                "    AND loc.NM_Local COLLATE utf8mb4_unicode_ci = " +
                                "        i.NM_LocalEncontrado COLLATE utf8mb4_unicode_ci AND loc.FG_Excluido = 0 " +
                                "  WHERE i.IDR_Evento = :ev AND i.FG_Excluido = 0 " +
                                "  AND i.DT_Cadastro >= :ini AND i.DT_Cadastro < :fim " +
                                "  GROUP BY COALESCE(i.IDR_LocalAtual, loc.ID_Local) " +
                                ") cnt ON cnt.idLocal = l.ID_Local " +
                                "WHERE l.IDR_Evento = :ev AND l.FG_Excluido = 0 AND l.FG_Ativo = 1 " +
                                "ORDER BY achados DESC, l.NM_Local", Tuple.class)
                .setParameter("ev", ev).setParameter("ini", j.inicio()).setParameter("fim", j.fim())
                .getResultList();

        long max = rows.stream().mapToLong(t -> PainelChartHelper.asLong(t.get("achados"))).max().orElse(1);
        if (max <= 0) max = 1;
        List<Map<String, Object>> out = new ArrayList<>();
        for (Tuple t : rows) {
            long achados = PainelChartHelper.asLong(t.get("achados"));
            double lat = t.get("lat") != null ? PainelChartHelper.asDouble(t.get("lat")) : 0;
            double lng = t.get("lng") != null ? PainelChartHelper.asDouble(t.get("lng")) : 0;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("nome", String.valueOf(t.get("nome")));
            m.put("lat", lat);
            m.put("lng", lng);
            m.put("achados", achados);
            m.put("intensidade", Math.min(1.0, achados / (double) max));
            out.add(m);
        }
        return out;
    }

    private List<Map<String, Object>> horariosSeries(Long ev, PainelChartHelper.Janela j) {
        Map<Integer, Long> achados = new HashMap<>();
        Map<Integer, Long> atend = new HashMap<>();
        @SuppressWarnings("unchecked")
        List<Tuple> aRows = em.createNativeQuery(
                        "SELECT HOUR(COALESCE(TIMESTAMP(i.DT_Encontrado, i.HR_Encontrado), i.DT_Cadastro)) h, COUNT(*) q " +
                                "FROM item i WHERE i.IDR_Evento = :ev AND i.FG_Excluido = 0 " +
                                "AND i.DT_Cadastro >= :ini AND i.DT_Cadastro < :fim GROUP BY h", Tuple.class)
                .setParameter("ev", ev).setParameter("ini", j.inicio()).setParameter("fim", j.fim())
                .getResultList();
        for (Tuple t : aRows) achados.put(((Number) t.get("h")).intValue(), PainelChartHelper.asLong(t.get("q")));

        @SuppressWarnings("unchecked")
        List<Tuple> dRows = em.createNativeQuery(
                        "SELECT HOUR(d.DT_Devolucao) h, COUNT(*) q FROM devolucao d " +
                                "WHERE d.IDR_Evento = :ev AND d.FG_Excluido = 0 AND d.FG_Concluido = 1 " +
                                "AND d.DT_Devolucao >= :ini AND d.DT_Devolucao < :fim GROUP BY h", Tuple.class)
                .setParameter("ev", ev).setParameter("ini", j.inicio()).setParameter("fim", j.fim())
                .getResultList();
        for (Tuple t : dRows) atend.put(((Number) t.get("h")).intValue(), PainelChartHelper.asLong(t.get("q")));

        @SuppressWarnings("unchecked")
        List<Tuple> cRows = em.createNativeQuery(
                        "SELECT HOUR(c.DT_Cadastro) h, COUNT(*) q FROM claim c " +
                                "WHERE c.IDR_Evento = :ev AND c.FG_Excluido = 0 " +
                                "AND c.DT_Cadastro >= :ini AND c.DT_Cadastro < :fim GROUP BY h", Tuple.class)
                .setParameter("ev", ev).setParameter("ini", j.inicio()).setParameter("fim", j.fim())
                .getResultList();
        for (Tuple t : cRows) {
            int h = ((Number) t.get("h")).intValue();
            atend.merge(h, PainelChartHelper.asLong(t.get("q")), Long::sum);
        }

        List<Map<String, Object>> serieAchados = new ArrayList<>();
        List<Map<String, Object>> serieAtend = new ArrayList<>();
        // Janela operacional típica de festival: 14h–01h; se houver dados fora, usa 0–23h
        final int[] horasPadrao = {14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 0, 1};
        boolean temDadosFora = achados.keySet().stream().anyMatch(h -> {
            for (int x : horasPadrao) if (x == h) return false;
            return true;
        });
        int[] horas;
        if (temDadosFora) {
            horas = new int[24];
            for (int i = 0; i < 24; i++) horas[i] = i;
        } else {
            horas = horasPadrao;
        }
        for (int h : horas) {
            String label = h + "h";
            serieAchados.add(Map.of("name", label, "value", achados.getOrDefault(h, 0L)));
            serieAtend.add(Map.of("name", label, "value", atend.getOrDefault(h, 0L)));
        }
        return List.of(
                Map.of("name", "Achados", "series", serieAchados),
                Map.of("name", "Atendimentos", "series", serieAtend));
    }

    private List<Map<String, Object>> slaEquipes(Long ev, PainelChartHelper.Janela j) {
        @SuppressWarnings("unchecked")
        List<Tuple> equipes = em.createNativeQuery(
                        "SELECT e.ID_Equipe id, e.NM_Equipe equipe, e.TP_Equipe tipo, e.DS_Responsabilidade resp " +
                                "FROM equipe e WHERE e.IDR_Evento = :ev AND e.FG_Excluido = 0 AND e.FG_Ativo = 1 " +
                                "ORDER BY e.NM_Equipe", Tuple.class)
                .setParameter("ev", ev).getResultList();
        if (equipes.isEmpty()) return List.of();

        List<Map<String, Object>> out = new ArrayList<>();
        for (Tuple t : equipes) {
            long idEquipe = PainelChartHelper.asLong(t.get("id"));
            String tipo = String.valueOf(t.get("tipo")).toUpperCase(Locale.ROOT);
            String etapa = etapaPorTipoEquipe(tipo);
            double metaPct = 85;
            if (tipo.contains("COLETA") || tipo.contains("CAMPO")) metaPct = 90;
            if (tipo.contains("ATEND")) metaPct = 90;

            long parados = countItensParadosEquipe(ev, idEquipe, etapa);
            double slaPct = calcSlaPctEquipe(ev, j, idEquipe, etapa);
            String gargalo = parados > 0
                    ? ("Fila em " + etapa.toLowerCase(PT) + " (" + parados + " itens)")
                    : "Sem gargalo relevante";

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("equipe", String.valueOf(t.get("equipe")));
            m.put("slaPct", (int) Math.round(slaPct));
            m.put("meta", (int) metaPct);
            m.put("gargalo", gargalo);
            m.put("itensParados", (int) parados);
            m.put("etapaCritica", etapa);
            out.add(m);
        }
        return out;
    }

    private String etapaPorTipoEquipe(String tipo) {
        if (tipo.contains("COLETA") || tipo.contains("CAMPO")) return "Cadastro";
        if (tipo.contains("TRIAG")) return "Triagem";
        if (tipo.contains("ESTOQUE") || tipo.contains("TRANSF")) return "Estoque";
        if (tipo.contains("DEVOL")) return "Devolução";
        if (tipo.contains("ATEND")) return "Atendimento";
        return "Operação";
    }

    private double calcSlaPctEquipe(Long ev, PainelChartHelper.Janela j, long idEquipe, String etapa) {
        // Usa registros de SLA vinculados a itens/claims do evento quando disponíveis
        Number n = (Number) em.createNativeQuery(
                        "SELECT CASE WHEN COUNT(*) = 0 THEN NULL ELSE " +
                                "  SUM(CASE WHEN sr.ST_Sla IN ('CONCLUIDO', 'OK', 'DENTRO') OR " +
                                "    (sr.DT_Conclusao IS NOT NULL AND sr.DT_Conclusao <= sr.DT_Limite) THEN 1 ELSE 0 END) * 100.0 / COUNT(*) " +
                                "END " +
                                "FROM sla_registro sr " +
                                "WHERE sr.FG_Excluido = 0 AND sr.DT_Inicio >= :ini AND sr.DT_Inicio < :fim " +
                                "AND ( " +
                                "  (sr.TP_Entidade = 'ITEM' AND EXISTS (SELECT 1 FROM item i WHERE i.ID_Item = sr.ID_Entidade AND i.IDR_Evento = :ev)) " +
                                "  OR (sr.TP_Entidade = 'CLAIM' AND EXISTS (SELECT 1 FROM claim c WHERE c.ID_Claim = sr.ID_Entidade AND c.IDR_Evento = :ev)) " +
                                "  OR (sr.TP_Entidade = 'DEVOLUCAO' AND EXISTS (SELECT 1 FROM devolucao d WHERE d.ID_Devolucao = sr.ID_Entidade AND d.IDR_Evento = :ev)) " +
                                ")")
                .setParameter("ev", ev).setParameter("ini", j.inicio()).setParameter("fim", j.fim())
                .getSingleResult();
        if (n != null) return Math.max(0, Math.min(100, n.doubleValue()));

        // Heurística: proporção de ações concluídas vs paradas da equipe
        long parados = countItensParadosEquipe(ev, idEquipe, etapa);
        long acoes = Math.max(1, countPeriodo(
                "SELECT COUNT(*) FROM item i JOIN equipe_usuario eu ON eu.IDR_Usuario = i.IDR_UsuarioCadastro " +
                        "WHERE i.IDR_Evento = :ev AND i.FG_Excluido = 0 AND eu.IDR_Equipe = " + idEquipe + " AND eu.FG_Excluido = 0 " +
                        "AND i.DT_Cadastro >= :ini AND i.DT_Cadastro < :fim", ev, j.inicio(), j.fim()));
        return Math.max(0, Math.min(100, 100.0 - (parados * 100.0 / (acoes + parados))));
    }

    private long countItensParadosEquipe(Long ev, long idEquipe, String etapa) {
        return switch (etapa) {
            case "Triagem" -> countStatus(ev, List.of("Em triagem", "Aguardando triagem"));
            case "Estoque" -> countStatus(ev, List.of("Em transporte para estoque"));
            case "Devolução" -> countStatus(ev, List.of("Aguardando retirada", "Com pedido de devolucao"));
            case "Cadastro" -> countStatus(ev, List.of("Encontrado", "Coletado"));
            default -> 0;
        };
    }

    private List<Map<String, Object>> itensParados(Long ev) {
        @SuppressWarnings("unchecked")
        List<Tuple> rows = em.createNativeQuery(
                        "SELECT i.CD_Item codigo, i.NM_Titulo item, s.NM_Status status, " +
                                "TIMESTAMPDIFF(DAY, COALESCE(i.DT_Alteracao, i.DT_Cadastro), NOW()) dias, " +
                                "COALESCE(e.NM_Equipe, 'Não informado') equipe " +
                                "FROM item i " +
                                "JOIN status_item s ON s.ID_Status = i.IDR_Status " +
                                "LEFT JOIN equipe_usuario eu ON eu.IDR_Usuario = i.IDR_UsuarioCadastro AND eu.FG_Excluido = 0 " +
                                "LEFT JOIN equipe e ON e.ID_Equipe = eu.IDR_Equipe AND e.IDR_Evento = :ev AND e.FG_Excluido = 0 " +
                                "WHERE i.IDR_Evento = :ev AND i.FG_Excluido = 0 " +
                                "AND s.NM_Status IN ('Em triagem', 'Aguardando triagem', 'Em transporte para estoque', " +
                                "  'Aguardando retirada', 'Com pedido de devolucao', 'Em estoque') " +
                                "AND TIMESTAMPDIFF(DAY, COALESCE(i.DT_Alteracao, i.DT_Cadastro), NOW()) >= 1 " +
                                "ORDER BY dias DESC LIMIT 30", Tuple.class)
                .setParameter("ev", ev).getResultList();

        List<Map<String, Object>> out = new ArrayList<>();
        for (Tuple t : rows) {
            String status = String.valueOf(t.get("status"));
            String etapa = switch (status) {
                case "Em triagem", "Aguardando triagem" -> "Triagem";
                case "Em transporte para estoque", "Em estoque" -> "Estoque";
                case "Aguardando retirada", "Com pedido de devolucao" -> "Devolução";
                default -> "Operação";
            };
            String motivo = switch (status) {
                case "Aguardando triagem" -> "Aguardando início da triagem";
                case "Em triagem" -> "Triagem em andamento";
                case "Em transporte para estoque" -> "Transferência sem confirmação de recebimento";
                case "Aguardando retirada" -> "Aguardando assinatura / retirada";
                case "Com pedido de devolucao" -> "Pedido de devolução pendente";
                case "Em estoque" -> "Em custódia — sem solicitação recente";
                default -> "Não informado";
            };
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("codigo", String.valueOf(t.get("codigo")));
            m.put("item", String.valueOf(t.get("item")));
            m.put("etapa", etapa);
            m.put("dias", (int) PainelChartHelper.asLong(t.get("dias")));
            m.put("equipe", String.valueOf(t.get("equipe")));
            m.put("motivo", motivo);
            out.add(m);
        }
        return out;
    }

    private List<Map<String, Object>> gargalos(List<Map<String, Object>> slaEquipes, List<Map<String, Object>> itensParados) {
        Map<String, long[]> agg = new LinkedHashMap<>();
        Map<String, String> extras = new LinkedHashMap<>();
        for (Map<String, Object> s : slaEquipes) {
            String etapa = String.valueOf(s.get("etapaCritica"));
            long parados = PainelChartHelper.asLong(s.get("itensParados"));
            agg.putIfAbsent(etapa, new long[]{0});
            agg.get(etapa)[0] += parados;
            extras.putIfAbsent(etapa, String.valueOf(s.get("gargalo")));
        }
        if (agg.isEmpty()) {
            Map<String, Long> porEtapa = new LinkedHashMap<>();
            for (Map<String, Object> p : itensParados) {
                porEtapa.merge(String.valueOf(p.get("etapa")), 1L, Long::sum);
            }
            List<Object[]> entries = new ArrayList<>();
            for (Map.Entry<String, Long> e : porEtapa.entrySet()) {
                entries.add(new Object[]{e.getKey(), e.getValue(), "derivado de itens parados"});
            }
            return PainelChartHelper.barsWithExtra(entries);
        }
        List<Object[]> entries = new ArrayList<>();
        for (Map.Entry<String, long[]> e : agg.entrySet()) {
            String extra = extras.getOrDefault(e.getKey(), "—");
            if (extra.startsWith("Fila em ")) {
                extra = extra.replaceFirst("Fila em [^(]+\\(", "").replaceAll("\\)$", "");
            } else if (extra.equals("Sem gargalo relevante")) {
                extra = "baixa demanda";
            }
            entries.add(new Object[]{e.getKey(), e.getValue()[0], extra});
        }
        entries.sort((a, b) -> Long.compare(((Number) b[1]).longValue(), ((Number) a[1]).longValue()));
        return PainelChartHelper.barsWithExtra(entries);
    }

    private List<Map<String, Object>> previsaoDiaSeries(Long ev, PainelChartHelper.Janela j) {
        LocalDate fim = j.fim().toLocalDate();
        LocalDate ini = j.inicio().toLocalDate();
        Map<LocalDate, Long> reais = contarPorDia(
                "SELECT DATE(i.DT_Cadastro) d, COUNT(*) q FROM item i WHERE i.IDR_Evento = :ev AND i.FG_Excluido = 0 " +
                        "AND DATE(i.DT_Cadastro) BETWEEN :ini AND :fim GROUP BY DATE(i.DT_Cadastro)",
                ev, ini, fim);

        List<Long> valores = new ArrayList<>();
        List<LocalDate> datas = new ArrayList<>();
        for (LocalDate d = ini; !d.isAfter(fim); d = d.plusDays(1)) {
            datas.add(d);
            valores.add(reais.getOrDefault(d, 0L));
        }

        List<Map<String, Object>> serieReal = new ArrayList<>();
        List<Map<String, Object>> seriePrev = new ArrayList<>();
        for (int i = 0; i < datas.size(); i++) {
            long real = valores.get(i);
            double previsto = mediaMovel(valores, i, 3);
            // tendência: diferença média dos últimos pontos
            if (i >= 2) {
                double trend = (valores.get(i) - valores.get(Math.max(0, i - 2))) / 2.0;
                previsto = Math.max(0, previsto + trend * 0.3);
            }
            String label = labelDia(datas.get(i));
            serieReal.add(Map.of("name", label, "value", real));
            seriePrev.add(Map.of("name", label, "value", Math.round(previsto)));
        }

        // Projeção de até 3 dias futuros (média móvel + tendência)
        double trend = 0;
        if (valores.size() >= 3) {
            trend = (valores.get(valores.size() - 1) - valores.get(valores.size() - 3)) / 2.0;
        }
        double base = valores.isEmpty() ? 0 : mediaMovel(valores, valores.size() - 1, 3);
        for (int k = 1; k <= 3; k++) {
            LocalDate d = fim.plusDays(k);
            double previsto = Math.max(0, base + trend * k);
            String label = labelDia(d);
            seriePrev.add(Map.of("name", label, "value", Math.round(previsto)));
        }

        return List.of(
                Map.of("name", "Realizado", "series", serieReal),
                Map.of("name", "Previsto", "series", seriePrev));
    }

    private List<Map<String, Object>> previsaoLocal(Long ev, PainelChartHelper.Janela j) {
        LocalDate hoje = TimeConfig.now().toLocalDate();
        LocalDate ontem = hoje.minusDays(1);
        LocalDate anteontem = hoje.minusDays(2);

        @SuppressWarnings("unchecked")
        List<Tuple> rows = em.createNativeQuery(
                        "SELECT COALESCE(NULLIF(i.NM_LocalEncontrado, ''), l.NM_Local, 'Não informado') local, " +
                                "SUM(CASE WHEN DATE(i.DT_Cadastro) = :hoje THEN 1 ELSE 0 END) hoje, " +
                                "SUM(CASE WHEN DATE(i.DT_Cadastro) = :ontem THEN 1 ELSE 0 END) ontem, " +
                                "SUM(CASE WHEN DATE(i.DT_Cadastro) = :ante THEN 1 ELSE 0 END) ante, " +
                                "COUNT(*) total " +
                                "FROM item i LEFT JOIN local l ON l.ID_Local = i.IDR_LocalAtual " +
                                "WHERE i.IDR_Evento = :ev AND i.FG_Excluido = 0 " +
                                "AND i.DT_Cadastro >= :ini AND i.DT_Cadastro < :fim " +
                                "GROUP BY COALESCE(NULLIF(i.NM_LocalEncontrado, ''), l.NM_Local, 'Não informado') " +
                                "ORDER BY total DESC LIMIT 8", Tuple.class)
                .setParameter("ev", ev).setParameter("ini", j.inicio()).setParameter("fim", j.fim())
                .setParameter("hoje", hoje).setParameter("ontem", ontem).setParameter("ante", anteontem)
                .getResultList();

        List<Map<String, Object>> out = new ArrayList<>();
        for (Tuple t : rows) {
            long h = PainelChartHelper.asLong(t.get("hoje"));
            long o = PainelChartHelper.asLong(t.get("ontem"));
            long a = PainelChartHelper.asLong(t.get("ante"));
            double media = (h + o + a) / 3.0;
            double trend = h - o;
            long previstoHoje = Math.round(Math.max(0, media));
            long previstoAmanha = Math.round(Math.max(0, media + trend * 0.5));
            // Confiança: quanto menor a dispersão, maior a confiança
            double media2 = Math.max(1, media);
            double var = (Math.pow(h - media, 2) + Math.pow(o - media, 2) + Math.pow(a - media, 2)) / 3.0;
            double cv = Math.sqrt(var) / media2;
            int confianca = (int) Math.max(50, Math.min(95, Math.round(95 - cv * 40)));
            String tendencia;
            if (trend > 1) tendencia = "alta";
            else if (trend < -1) tendencia = "baixa";
            else tendencia = "estável";

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("local", String.valueOf(t.get("local")));
            m.put("previstoHoje", previstoHoje);
            m.put("previstoAmanha", previstoAmanha);
            m.put("confianca", confianca);
            m.put("tendencia", tendencia);
            out.add(m);
        }
        return out;
    }

    // ------------------------------------------------------------------

    private static double mediaMovel(List<Long> valores, int idx, int janela) {
        int from = Math.max(0, idx - janela + 1);
        double sum = 0;
        int n = 0;
        for (int i = from; i <= idx; i++) {
            sum += valores.get(i);
            n++;
        }
        return n == 0 ? 0 : sum / n;
    }

    private static String labelDia(LocalDate d) {
        String dow = d.getDayOfWeek().getDisplayName(TextStyle.SHORT, PT);
        dow = dow.substring(0, 1).toUpperCase(PT) + dow.substring(1);
        return dow + " " + d.getDayOfMonth();
    }

    private static String deltaItens(long atual, long anterior) {
        long d = atual - anterior;
        String sign = d > 0 ? "+" : "";
        return sign + d + " itens";
    }

    private long countPeriodo(String sql, Long ev, LocalDateTime ini, LocalDateTime fim) {
        Number n = (Number) em.createNativeQuery(sql)
                .setParameter("ev", ev).setParameter("ini", ini).setParameter("fim", fim)
                .getSingleResult();
        return n == null ? 0L : n.longValue();
    }

    private long countStatus(Long ev, List<String> status) {
        if (status == null || status.isEmpty()) return 0L;
        StringBuilder in = new StringBuilder();
        for (int i = 0; i < status.size(); i++) {
            if (i > 0) in.append(',');
            in.append(":st").append(i);
        }
        var q = em.createNativeQuery(
                "SELECT COUNT(*) FROM item i JOIN status_item s ON s.ID_Status = i.IDR_Status " +
                        "WHERE i.IDR_Evento = :ev AND i.FG_Excluido = 0 AND s.NM_Status IN (" + in + ")");
        q.setParameter("ev", ev);
        for (int i = 0; i < status.size(); i++) q.setParameter("st" + i, status.get(i));
        Number n = (Number) q.getSingleResult();
        return n == null ? 0L : n.longValue();
    }

    /** Aproximação do estoque “anterior”: itens cadastrados até o fim do período anterior ainda nesses status. */
    private long countStatusSnapshotPrev(Long ev, List<String> status, PainelChartHelper.Janela j) {
        if (status == null || status.isEmpty()) return 0L;
        StringBuilder in = new StringBuilder();
        for (int i = 0; i < status.size(); i++) {
            if (i > 0) in.append(',');
            in.append(":st").append(i);
        }
        var q = em.createNativeQuery(
                "SELECT COUNT(*) FROM item i JOIN status_item s ON s.ID_Status = i.IDR_Status " +
                        "WHERE i.IDR_Evento = :ev AND i.FG_Excluido = 0 AND s.NM_Status IN (" + in + ") " +
                        "AND i.DT_Cadastro < :fim");
        q.setParameter("ev", ev).setParameter("fim", j.prevFim());
        for (int i = 0; i < status.size(); i++) q.setParameter("st" + i, status.get(i));
        Number n = (Number) q.getSingleResult();
        return n == null ? 0L : n.longValue();
    }

    private double avgHours(String sql, Long ev, PainelChartHelper.Janela j) {
        Object r = em.createNativeQuery(sql)
                .setParameter("ev", ev).setParameter("ini", j.inicio()).setParameter("fim", j.fim())
                .getSingleResult();
        return r == null ? 0d : ((Number) r).doubleValue();
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
