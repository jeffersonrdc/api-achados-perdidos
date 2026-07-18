package br.com.achadosperdidos.service;

import br.com.achadosperdidos.config.TimeConfig;
import br.com.achadosperdidos.entity.Evento;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.repository.EventoRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Relatorios operacionais (secao 12 da especificacao). Reaproveita as views
 * ja existentes no banco. Recebe idEvento assinado; retorna linhas como mapas
 * coluna->valor (superficie somente-leitura para paineis/exportacao).
 */
@Service
public class RelatorioService {

    private static final Set<String> TIPOS = Set.of(
            "encontrados", "devolvidos", "estoque", "pedidos",
            "transferencias", "produtividade", "auditoria", "lgpd");

    private static final int ROW_LIMIT = 200;

    @PersistenceContext
    private EntityManager em;

    private final EventoRepository eventoRepository;
    private final SignedResourceIdCodec idCodec;

    public RelatorioService(EventoRepository eventoRepository, SignedResourceIdCodec idCodec) {
        this.eventoRepository = eventoRepository;
        this.idCodec = idCodec;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> painel(String tipo, String idEvento, int dias) {
        String t = tipo == null ? "" : tipo.trim().toLowerCase(Locale.ROOT);
        if (!TIPOS.contains(t)) {
            throw new IllegalArgumentException(
                    "Tipo de relatório inválido. Use: encontrados, devolvidos, estoque, pedidos, transferencias, produtividade, auditoria, lgpd.");
        }
        Long ev = idCodec.decodeEventoId(idEvento);
        Evento evento = eventoRepository.findById(ev)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento não encontrado."));
        PainelChartHelper.Janela janela = PainelChartHelper.janela(evento, dias);

        return switch (t) {
            case "encontrados" -> painelEncontrados(ev, janela);
            case "devolvidos" -> painelDevolvidos(ev, janela);
            case "estoque" -> painelEstoque(ev, janela);
            case "pedidos" -> painelPedidos(ev, janela);
            case "transferencias" -> painelTransferencias(ev, janela);
            case "produtividade" -> painelProdutividade(ev, janela);
            case "auditoria" -> painelAuditoria(ev, janela);
            case "lgpd" -> painelLgpd(ev, janela, evento);
            default -> throw new IllegalArgumentException("Tipo de relatório inválido.");
        };
    }

    // ------------------------------------------------------------------
    // Painéis por tipo
    // ------------------------------------------------------------------

    private Map<String, Object> painelEncontrados(Long ev, PainelChartHelper.Janela j) {
        Map<String, Long> porStatus = contarMap(
                "SELECT COALESCE(s.NM_Status, 'Não informado') n, COUNT(*) q FROM item i " +
                        "JOIN status_item s ON s.ID_Status = i.IDR_Status " +
                        "WHERE i.IDR_Evento = :ev AND i.FG_Excluido = 0 " +
                        "AND i.DT_Cadastro >= :ini AND i.DT_Cadastro < :fim GROUP BY s.NM_Status ORDER BY q DESC",
                ev, j.inicio(), j.fim());

        long total = porStatus.values().stream().mapToLong(Long::longValue).sum();
        long emEstoque = porStatus.getOrDefault("Em estoque", 0L);
        long emTriagem = porStatus.getOrDefault("Em triagem", 0L) + porStatus.getOrDefault("Aguardando triagem", 0L);
        long devolvidos = porStatus.getOrDefault("Devolvido", 0L) + porStatus.getOrDefault("Finalizado", 0L);

        List<Map.Entry<String, Number>> catEntries = entriesFrom(
                "SELECT COALESCE(c.NM_Categoria, 'Não informado') n, COUNT(*) q FROM item i " +
                        "LEFT JOIN categoria c ON c.ID_Categoria = i.IDR_Categoria " +
                        "WHERE i.IDR_Evento = :ev AND i.FG_Excluido = 0 " +
                        "AND i.DT_Cadastro >= :ini AND i.DT_Cadastro < :fim GROUP BY c.NM_Categoria ORDER BY q DESC",
                ev, j.inicio(), j.fim());
        List<Map.Entry<String, Number>> localEntries = entriesFrom(
                "SELECT COALESCE(NULLIF(i.NM_LocalEncontrado, ''), l.NM_Local, 'Não informado') n, COUNT(*) q FROM item i " +
                        "LEFT JOIN local l ON l.ID_Local = i.IDR_LocalAtual " +
                        "WHERE i.IDR_Evento = :ev AND i.FG_Excluido = 0 " +
                        "AND i.DT_Cadastro >= :ini AND i.DT_Cadastro < :fim " +
                        "GROUP BY COALESCE(NULLIF(i.NM_LocalEncontrado, ''), l.NM_Local, 'Não informado') ORDER BY q DESC LIMIT 12",
                ev, j.inicio(), j.fim());
        List<Map.Entry<String, Number>> opEntries = entriesFrom(
                "SELECT COALESCE(u.NM_Usuario, 'Não informado') n, COUNT(*) q FROM item i " +
                        "LEFT JOIN usuario u ON u.ID_Usuario = i.IDR_UsuarioCadastro " +
                        "WHERE i.IDR_Evento = :ev AND i.FG_Excluido = 0 " +
                        "AND i.DT_Cadastro >= :ini AND i.DT_Cadastro < :fim GROUP BY u.NM_Usuario ORDER BY q DESC LIMIT 12",
                ev, j.inicio(), j.fim());

        @SuppressWarnings("unchecked")
        List<Tuple> rows = em.createNativeQuery(
                        "SELECT i.CD_Item codigo, i.NM_Titulo item, COALESCE(c.NM_Categoria, 'Não informado') categoria, " +
                                "COALESCE(NULLIF(i.NM_LocalEncontrado, ''), l.NM_Local, 'Não informado') local, " +
                                "COALESCE(u.NM_Usuario, 'Não informado') operador, " +
                                "COALESCE(s.NM_Status, '—') status, i.DT_Cadastro data " +
                                "FROM item i " +
                                "LEFT JOIN categoria c ON c.ID_Categoria = i.IDR_Categoria " +
                                "LEFT JOIN local l ON l.ID_Local = i.IDR_LocalAtual " +
                                "LEFT JOIN usuario u ON u.ID_Usuario = i.IDR_UsuarioCadastro " +
                                "LEFT JOIN status_item s ON s.ID_Status = i.IDR_Status " +
                                "WHERE i.IDR_Evento = :ev AND i.FG_Excluido = 0 " +
                                "AND i.DT_Cadastro >= :ini AND i.DT_Cadastro < :fim " +
                                "ORDER BY i.DT_Cadastro DESC LIMIT " + ROW_LIMIT, Tuple.class)
                .setParameter("ev", ev).setParameter("ini", j.inicio()).setParameter("fim", j.fim())
                .getResultList();

        List<Map<String, Object>> rowMaps = new ArrayList<>();
        for (Tuple t : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("codigo", str(t.get("codigo")));
            m.put("item", str(t.get("item")));
            m.put("categoria", str(t.get("categoria")));
            m.put("local", str(t.get("local")));
            m.put("operador", str(t.get("operador")));
            m.put("status", str(t.get("status")));
            m.put("data", PainelChartHelper.fmt(toLdt(t.get("data"))));
            rowMaps.add(m);
        }

        Map<String, Object> out = base(j);
        out.put("kpis", List.of(
                PainelChartHelper.kpi("Total no período", String.valueOf(total),
                        "bg-indigo-50 dark:bg-indigo-500/20", "text-indigo-500"),
                PainelChartHelper.kpi("Em estoque", String.valueOf(emEstoque),
                        "bg-emerald-50 dark:bg-emerald-500/20", "text-emerald-500"),
                PainelChartHelper.kpi("Em triagem", String.valueOf(emTriagem),
                        "bg-amber-50 dark:bg-amber-500/20", "text-amber-500"),
                PainelChartHelper.kpi("Devolvidos", String.valueOf(devolvidos),
                        "bg-purple-50 dark:bg-purple-500/20", "text-purple-500")));
        out.put("status", PainelChartHelper.slices(toNumberEntries(porStatus)));
        out.put("categoria", PainelChartHelper.slices(catEntries));
        out.put("local", PainelChartHelper.bars(localEntries));
        out.put("operador", PainelChartHelper.bars(opEntries));
        out.put("rows", rowMaps);
        return out;
    }

    private Map<String, Object> painelDevolvidos(Long ev, PainelChartHelper.Janela j) {
        long total = count(
                "SELECT COUNT(*) FROM devolucao d WHERE d.IDR_Evento = :ev AND d.FG_Excluido = 0 AND d.FG_Concluido = 1 " +
                        "AND d.DT_Devolucao >= :ini AND d.DT_Devolucao < :fim", ev, j.inicio(), j.fim());
        long comTermo = count(
                "SELECT COUNT(*) FROM devolucao d WHERE d.IDR_Evento = :ev AND d.FG_Excluido = 0 AND d.FG_Concluido = 1 " +
                        "AND d.FG_Assinado = 1 AND d.DT_Devolucao >= :ini AND d.DT_Devolucao < :fim", ev, j.inicio(), j.fim());
        LocalDateTime inicioHoje = TimeConfig.now().toLocalDate().atStartOfDay();
        long hoje = count(
                "SELECT COUNT(*) FROM devolucao d WHERE d.IDR_Evento = :ev AND d.FG_Excluido = 0 AND d.FG_Concluido = 1 " +
                        "AND d.DT_Devolucao >= :ini AND d.DT_Devolucao < :fim", ev, inicioHoje, TimeConfig.now().plusSeconds(1));
        long operadores = count(
                "SELECT COUNT(DISTINCT COALESCE(h.IDR_Operador, 0)) FROM devolucao d " +
                        "LEFT JOIN claim_historico h ON h.IDR_Claim = d.IDR_Claim AND h.FG_Excluido = 0 " +
                        "AND h.TP_Evento IN ('ENTREGA', 'CONCLUIDO', 'ASSINATURA', 'DEVOLUCAO') " +
                        "WHERE d.IDR_Evento = :ev AND d.FG_Excluido = 0 AND d.FG_Concluido = 1 " +
                        "AND d.DT_Devolucao >= :ini AND d.DT_Devolucao < :fim", ev, j.inicio(), j.fim());

        List<Map.Entry<String, Number>> opEntries = entriesFrom(
                "SELECT COALESCE(u.NM_Usuario, 'Não informado') n, COUNT(*) q FROM devolucao d " +
                        "LEFT JOIN claim_historico h ON h.IDR_Claim = d.IDR_Claim AND h.FG_Excluido = 0 " +
                        "AND h.TP_Evento IN ('ENTREGA', 'CONCLUIDO', 'ASSINATURA', 'DEVOLUCAO', 'APROVACAO') " +
                        "LEFT JOIN usuario u ON u.ID_Usuario = h.IDR_Operador " +
                        "WHERE d.IDR_Evento = :ev AND d.FG_Excluido = 0 AND d.FG_Concluido = 1 " +
                        "AND d.DT_Devolucao >= :ini AND d.DT_Devolucao < :fim " +
                        "GROUP BY u.NM_Usuario ORDER BY q DESC LIMIT 12",
                ev, j.inicio(), j.fim());

        @SuppressWarnings("unchecked")
        List<Tuple> rows = em.createNativeQuery(
                        "SELECT d.ID_Devolucao id, i.NM_Titulo item, d.NM_Recebedor responsavel, d.NR_CPF cpf, " +
                                "d.DT_Devolucao data, d.FG_Assinado assinado, " +
                                "COALESCE(u.NM_Usuario, 'Não informado') operador, " +
                                "COALESCE(l.NM_Local, i.NM_LocalEncontrado, 'Não informado') local " +
                                "FROM devolucao d " +
                                "JOIN item i ON i.ID_Item = d.IDR_Item " +
                                "LEFT JOIN local l ON l.ID_Local = i.IDR_LocalAtual " +
                                "LEFT JOIN ( " +
                                "  SELECT h1.IDR_Claim, h1.IDR_Operador FROM claim_historico h1 " +
                                "  INNER JOIN (SELECT IDR_Claim, MAX(ID_ClaimHistorico) mx FROM claim_historico " +
                                "    WHERE FG_Excluido = 0 AND IDR_Operador IS NOT NULL GROUP BY IDR_Claim) x " +
                                "    ON x.mx = h1.ID_ClaimHistorico " +
                                ") ho ON ho.IDR_Claim = d.IDR_Claim " +
                                "LEFT JOIN usuario u ON u.ID_Usuario = ho.IDR_Operador " +
                                "WHERE d.IDR_Evento = :ev AND d.FG_Excluido = 0 AND d.FG_Concluido = 1 " +
                                "AND d.DT_Devolucao >= :ini AND d.DT_Devolucao < :fim " +
                                "ORDER BY d.DT_Devolucao DESC LIMIT " + ROW_LIMIT, Tuple.class)
                .setParameter("ev", ev).setParameter("ini", j.inicio()).setParameter("fim", j.fim())
                .getResultList();

        List<Map<String, Object>> rowMaps = new ArrayList<>();
        for (Tuple t : rows) {
            long id = PainelChartHelper.asLong(t.get("id"));
            boolean assinado = Boolean.TRUE.equals(t.get("assinado"))
                    || (t.get("assinado") instanceof Number n && n.intValue() == 1);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("proto", String.format("DEV-%d-%05d", toLdt(t.get("data")).getYear(), id));
            m.put("item", str(t.get("item")));
            m.put("responsavel", PainelChartHelper.nvl(str(t.get("responsavel"))));
            m.put("documento", PainelChartHelper.maskCpf(str(t.get("cpf"))));
            m.put("data", PainelChartHelper.fmt(toLdt(t.get("data"))));
            m.put("operador", str(t.get("operador")));
            m.put("termo", assinado ? String.format("TERMO-DEV-%05d", id) : "—");
            m.put("local", str(t.get("local")));
            rowMaps.add(m);
        }

        Map<String, Object> out = base(j);
        out.put("kpis", List.of(
                PainelChartHelper.kpi("Devoluções", String.valueOf(total),
                        "bg-emerald-50 dark:bg-emerald-500/20", "text-emerald-500"),
                PainelChartHelper.kpi("Com termo PDF", String.valueOf(comTermo),
                        "bg-indigo-50 dark:bg-indigo-500/20", "text-indigo-500"),
                PainelChartHelper.kpi("Hoje", String.valueOf(hoje),
                        "bg-amber-50 dark:bg-amber-500/20", "text-amber-500"),
                PainelChartHelper.kpi("Operadores", String.valueOf(operadores),
                        "bg-purple-50 dark:bg-purple-500/20", "text-purple-500")));
        out.put("operador", PainelChartHelper.bars(opEntries));
        out.put("rows", rowMaps);
        return out;
    }

    private Map<String, Object> painelEstoque(Long ev, PainelChartHelper.Janela j) {
        String estoqueStatus = "s.NM_Status IN ('Em estoque', 'Com pedido de devolucao', 'Aguardando retirada')";

        @SuppressWarnings("unchecked")
        List<Tuple> stats = em.createNativeQuery(
                        "SELECT COUNT(*) total, " +
                                "AVG(TIMESTAMPDIFF(DAY, i.DT_Cadastro, NOW())) media, " +
                                "SUM(CASE WHEN TIMESTAMPDIFF(DAY, i.DT_Cadastro, NOW()) > 30 THEN 1 ELSE 0 END) mais30, " +
                                "COUNT(DISTINCT dep.ID_Deposito) depositos " +
                                "FROM item i " +
                                "JOIN status_item s ON s.ID_Status = i.IDR_Status " +
                                "LEFT JOIN localizacao loc ON loc.ID_Localizacao = i.IDR_Localizacao " +
                                "LEFT JOIN deposito dep ON dep.ID_Deposito = loc.IDR_Deposito AND dep.FG_Excluido = 0 " +
                                "WHERE i.IDR_Evento = :ev AND i.FG_Excluido = 0 AND " + estoqueStatus, Tuple.class)
                .setParameter("ev", ev).getResultList();
        Tuple st = stats.isEmpty() ? null : stats.get(0);
        long total = st == null ? 0 : PainelChartHelper.asLong(st.get("total"));
        double media = st == null ? 0 : PainelChartHelper.asDouble(st.get("media"));
        long mais30 = st == null ? 0 : PainelChartHelper.asLong(st.get("mais30"));
        long depositos = st == null ? 0 : PainelChartHelper.asLong(st.get("depositos"));

        List<Map.Entry<String, Number>> localEntries = entriesFromNoPeriod(
                "SELECT CONCAT(COALESCE(dep.NM_Deposito, 'Sem depósito'), ' · ', COALESCE(loc.NM_Setor, 'Sem setor')) n, COUNT(*) q " +
                        "FROM item i JOIN status_item s ON s.ID_Status = i.IDR_Status " +
                        "LEFT JOIN localizacao loc ON loc.ID_Localizacao = i.IDR_Localizacao " +
                        "LEFT JOIN deposito dep ON dep.ID_Deposito = loc.IDR_Deposito " +
                        "WHERE i.IDR_Evento = :ev AND i.FG_Excluido = 0 AND " + estoqueStatus + " " +
                        "GROUP BY dep.NM_Deposito, loc.NM_Setor ORDER BY q DESC LIMIT 12", ev);

        @SuppressWarnings("unchecked")
        List<Tuple> permRows = em.createNativeQuery(
                        "SELECT CASE " +
                                "WHEN TIMESTAMPDIFF(DAY, i.DT_Cadastro, NOW()) <= 2 THEN '0–2 dias' " +
                                "WHEN TIMESTAMPDIFF(DAY, i.DT_Cadastro, NOW()) <= 7 THEN '3–7 dias' " +
                                "WHEN TIMESTAMPDIFF(DAY, i.DT_Cadastro, NOW()) <= 15 THEN '8–15 dias' " +
                                "WHEN TIMESTAMPDIFF(DAY, i.DT_Cadastro, NOW()) <= 30 THEN '16–30 dias' " +
                                "ELSE '+30 dias' END n, COUNT(*) q " +
                                "FROM item i JOIN status_item s ON s.ID_Status = i.IDR_Status " +
                                "WHERE i.IDR_Evento = :ev AND i.FG_Excluido = 0 AND " + estoqueStatus + " " +
                                "GROUP BY n ORDER BY MIN(TIMESTAMPDIFF(DAY, i.DT_Cadastro, NOW()))", Tuple.class)
                .setParameter("ev", ev).getResultList();
        List<Map.Entry<String, Number>> permEntries = new ArrayList<>();
        for (Tuple t : permRows) {
            permEntries.add(entry(str(t.get("n")), PainelChartHelper.asLong(t.get("q"))));
        }

        @SuppressWarnings("unchecked")
        List<Tuple> rows = em.createNativeQuery(
                        "SELECT i.CD_Item codigo, i.NM_Titulo item, COALESCE(dep.NM_Deposito, 'Não informado') deposito, " +
                                "COALESCE(loc.NM_Setor, '—') setor, " +
                                "TRIM(BOTH ' · ' FROM CONCAT_WS(' · ', " +
                                "  NULLIF(loc.NM_Estante, ''), NULLIF(loc.NM_Prateleira, ''), " +
                                "  NULLIF(loc.NM_Caixa, ''), NULLIF(loc.NM_Posicao, ''))) posicao, " +
                                "TIMESTAMPDIFF(DAY, i.DT_Cadastro, NOW()) dias, s.NM_Status status " +
                                "FROM item i JOIN status_item s ON s.ID_Status = i.IDR_Status " +
                                "LEFT JOIN localizacao loc ON loc.ID_Localizacao = i.IDR_Localizacao " +
                                "LEFT JOIN deposito dep ON dep.ID_Deposito = loc.IDR_Deposito " +
                                "WHERE i.IDR_Evento = :ev AND i.FG_Excluido = 0 AND " + estoqueStatus + " " +
                                "ORDER BY dias DESC LIMIT " + ROW_LIMIT, Tuple.class)
                .setParameter("ev", ev).getResultList();

        List<Map<String, Object>> rowMaps = new ArrayList<>();
        for (Tuple t : rows) {
            String stName = str(t.get("status"));
            String statusPainel = switch (stName) {
                case "Com pedido de devolucao", "Aguardando retirada" -> "Reservado";
                default -> "Disponível";
            };
            String pos = str(t.get("posicao"));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("codigo", str(t.get("codigo")));
            m.put("item", str(t.get("item")));
            m.put("deposito", str(t.get("deposito")));
            m.put("setor", str(t.get("setor")));
            m.put("posicao", (pos == null || pos.isBlank()) ? "—" : pos);
            m.put("dias", (int) PainelChartHelper.asLong(t.get("dias")));
            m.put("status", statusPainel);
            rowMaps.add(m);
        }

        Map<String, Object> out = base(j);
        out.put("kpis", List.of(
                PainelChartHelper.kpi("Itens em estoque", String.valueOf(total),
                        "bg-indigo-50 dark:bg-indigo-500/20", "text-indigo-500"),
                PainelChartHelper.kpi("Permanência média", PainelChartHelper.fmtDecimalBr(media) + " d",
                        "bg-amber-50 dark:bg-amber-500/20", "text-amber-500"),
                PainelChartHelper.kpi("+30 dias", String.valueOf(mais30),
                        "bg-red-50 dark:bg-red-500/20", "text-red-500"),
                PainelChartHelper.kpi("Depósitos", String.valueOf(depositos),
                        "bg-purple-50 dark:bg-purple-500/20", "text-purple-500")));
        out.put("local", PainelChartHelper.bars(localEntries));
        out.put("permanencia", PainelChartHelper.slices(permEntries));
        out.put("rows", rowMaps);
        return out;
    }

    private Map<String, Object> painelPedidos(Long ev, PainelChartHelper.Janela j) {
        Map<String, Long> porStatus = contarMap(
                "SELECT COALESCE(s.NM_Status, 'Não informado') n, COUNT(*) q FROM claim c " +
                        "JOIN status_item s ON s.ID_Status = c.IDR_Status " +
                        "WHERE c.IDR_Evento = :ev AND c.FG_Excluido = 0 " +
                        "AND c.DT_Cadastro >= :ini AND c.DT_Cadastro < :fim GROUP BY s.NM_Status ORDER BY q DESC",
                ev, j.inicio(), j.fim());

        long total = porStatus.values().stream().mapToLong(Long::longValue).sum();
        long aprovados = porStatus.getOrDefault("Claim Aprovado", 0L);
        long pendencias = porStatus.getOrDefault("Claim Aguardando Info", 0L);
        long rejeitados = porStatus.getOrDefault("Claim Rejeitado", 0L);

        Map<String, Long> statusPainel = new LinkedHashMap<>();
        for (Map.Entry<String, Long> e : porStatus.entrySet()) {
            String label = PainelChartHelper.statusClaimPainel(e.getKey());
            statusPainel.merge(label, e.getValue(), Long::sum);
        }

        @SuppressWarnings("unchecked")
        List<Tuple> rows = em.createNativeQuery(
                        "SELECT COALESCE(c.CD_Claim, CONCAT('CLM-', YEAR(c.DT_Cadastro), '-', LPAD(c.ID_Claim, 5, '0'))) proto, " +
                                "c.NM_Nome solicitante, c.NM_Objeto item, s.NM_Status status, " +
                                "COALESCE(ua.NM_Usuario, '—') aprovador, c.DT_Cadastro data, " +
                                "COALESCE(c.DS_JustificativaReprovacao, c.DS_JustificativaAprovacao) motivo " +
                                "FROM claim c " +
                                "JOIN status_item s ON s.ID_Status = c.IDR_Status " +
                                "LEFT JOIN ( " +
                                "  SELECT h.IDR_Claim, h.IDR_Operador FROM claim_historico h " +
                                "  INNER JOIN (SELECT IDR_Claim, MAX(ID_ClaimHistorico) mx FROM claim_historico " +
                                "    WHERE FG_Excluido = 0 AND TP_Evento IN ('APROVACAO','REPROVACAO','APROVADO','REJEITADO') " +
                                "    GROUP BY IDR_Claim) x ON x.mx = h.ID_ClaimHistorico " +
                                ") ha ON ha.IDR_Claim = c.ID_Claim " +
                                "LEFT JOIN usuario ua ON ua.ID_Usuario = ha.IDR_Operador " +
                                "WHERE c.IDR_Evento = :ev AND c.FG_Excluido = 0 " +
                                "AND c.DT_Cadastro >= :ini AND c.DT_Cadastro < :fim " +
                                "ORDER BY c.DT_Cadastro DESC LIMIT " + ROW_LIMIT, Tuple.class)
                .setParameter("ev", ev).setParameter("ini", j.inicio()).setParameter("fim", j.fim())
                .getResultList();

        List<Map<String, Object>> rowMaps = new ArrayList<>();
        for (Tuple t : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("proto", str(t.get("proto")));
            m.put("solicitante", PainelChartHelper.nvl(str(t.get("solicitante"))));
            m.put("item", str(t.get("item")));
            m.put("status", PainelChartHelper.statusClaimPainel(str(t.get("status"))));
            m.put("aprovador", PainelChartHelper.nvlDash(str(t.get("aprovador"))));
            m.put("data", PainelChartHelper.fmt(toLdt(t.get("data"))));
            String motivo = str(t.get("motivo"));
            if (motivo != null && !motivo.isBlank()) m.put("motivo", motivo);
            rowMaps.add(m);
        }

        Map<String, Object> out = base(j);
        out.put("kpis", List.of(
                PainelChartHelper.kpi("Total pedidos", String.valueOf(total),
                        "bg-indigo-50 dark:bg-indigo-500/20", "text-indigo-500"),
                PainelChartHelper.kpi("Aprovados", String.valueOf(aprovados),
                        "bg-emerald-50 dark:bg-emerald-500/20", "text-emerald-500"),
                PainelChartHelper.kpi("Pendências", String.valueOf(pendencias),
                        "bg-amber-50 dark:bg-amber-500/20", "text-amber-500"),
                PainelChartHelper.kpi("Rejeitados", String.valueOf(rejeitados),
                        "bg-red-50 dark:bg-red-500/20", "text-red-500")));
        out.put("status", PainelChartHelper.slices(toNumberEntries(statusPainel)));
        out.put("rows", rowMaps);
        return out;
    }

    private Map<String, Object> painelTransferencias(Long ev, PainelChartHelper.Janela j) {
        Map<String, Long> porStatus = contarMap(
                "SELECT COALESCE(t.TP_Status, '—') n, COUNT(*) q FROM transferencia t " +
                        "WHERE t.IDR_Evento = :ev AND t.FG_Excluido = 0 " +
                        "AND t.DT_Transferencia >= :ini AND t.DT_Transferencia < :fim GROUP BY t.TP_Status",
                ev, j.inicio(), j.fim());
        long total = porStatus.values().stream().mapToLong(Long::longValue).sum();
        long concluidas = porStatus.getOrDefault("CONCLUIDA", 0L);
        long divergencias = porStatus.getOrDefault("DIVERGENCIA", 0L);
        long emTransito = porStatus.getOrDefault("EM_TRANSITO", 0L);

        List<Map.Entry<String, Number>> fluxoEntries = entriesFrom(
                "SELECT CONCAT(COALESCE(lo.NM_Local, '—'), ' → ', COALESCE(ld.NM_Local, '—')) n, COUNT(*) q " +
                        "FROM transferencia t " +
                        "LEFT JOIN local lo ON lo.ID_Local = t.IDR_LocalOrigem " +
                        "LEFT JOIN local ld ON ld.ID_Local = t.IDR_LocalDestino " +
                        "WHERE t.IDR_Evento = :ev AND t.FG_Excluido = 0 " +
                        "AND t.DT_Transferencia >= :ini AND t.DT_Transferencia < :fim " +
                        "GROUP BY lo.NM_Local, ld.NM_Local ORDER BY q DESC LIMIT 12",
                ev, j.inicio(), j.fim());

        @SuppressWarnings("unchecked")
        List<Tuple> rows = em.createNativeQuery(
                        "SELECT CONCAT('TRF-', YEAR(t.DT_Transferencia), '-', LPAD(t.ID_Transferencia, 5, '0')) codigo, " +
                                "i.NM_Titulo item, COALESCE(lo.NM_Local, '—') origem, COALESCE(ld.NM_Local, '—') destino, " +
                                "COALESCE(u.NM_Usuario, 'Não informado') responsavel, t.DT_Transferencia data, " +
                                "t.DS_Motivo motivo, t.TP_Status status " +
                                "FROM transferencia t " +
                                "JOIN item i ON i.ID_Item = t.IDR_Item " +
                                "LEFT JOIN local lo ON lo.ID_Local = t.IDR_LocalOrigem " +
                                "LEFT JOIN local ld ON ld.ID_Local = t.IDR_LocalDestino " +
                                "LEFT JOIN usuario u ON u.ID_Usuario = t.IDR_UsuarioResponsavel " +
                                "WHERE t.IDR_Evento = :ev AND t.FG_Excluido = 0 " +
                                "AND t.DT_Transferencia >= :ini AND t.DT_Transferencia < :fim " +
                                "ORDER BY t.DT_Transferencia DESC LIMIT " + ROW_LIMIT, Tuple.class)
                .setParameter("ev", ev).setParameter("ini", j.inicio()).setParameter("fim", j.fim())
                .getResultList();

        List<Map<String, Object>> rowMaps = new ArrayList<>();
        for (Tuple t : rows) {
            String status = PainelChartHelper.statusTransferenciaPainel(str(t.get("status")));
            String motivo = str(t.get("motivo"));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("codigo", str(t.get("codigo")));
            m.put("item", str(t.get("item")));
            m.put("origem", str(t.get("origem")));
            m.put("destino", str(t.get("destino")));
            m.put("responsavel", str(t.get("responsavel")));
            m.put("data", PainelChartHelper.fmt(toLdt(t.get("data"))));
            m.put("divergencia", "Divergência".equals(status) ? PainelChartHelper.nvl(motivo) : null);
            m.put("status", status);
            rowMaps.add(m);
        }

        Map<String, Object> out = base(j);
        out.put("kpis", List.of(
                PainelChartHelper.kpi("Transferências", String.valueOf(total),
                        "bg-indigo-50 dark:bg-indigo-500/20", "text-indigo-500"),
                PainelChartHelper.kpi("Concluídas", String.valueOf(concluidas),
                        "bg-emerald-50 dark:bg-emerald-500/20", "text-emerald-500"),
                PainelChartHelper.kpi("Divergências", String.valueOf(divergencias),
                        "bg-red-50 dark:bg-red-500/20", "text-red-500"),
                PainelChartHelper.kpi("Em trânsito", String.valueOf(emTransito),
                        "bg-amber-50 dark:bg-amber-500/20", "text-amber-500")));
        out.put("fluxo", PainelChartHelper.bars(fluxoEntries));
        out.put("rows", rowMaps);
        return out;
    }

    private Map<String, Object> painelProdutividade(Long ev, PainelChartHelper.Janela j) {
        @SuppressWarnings("unchecked")
        List<Tuple> equipes = em.createNativeQuery(
                        "SELECT e.NM_Equipe equipe, e.TP_Equipe tipo, " +
                                "COUNT(DISTINCT eu.IDR_Usuario) membros, " +
                                "(SELECT COUNT(*) FROM item i WHERE i.IDR_Evento = :ev AND i.FG_Excluido = 0 " +
                                "  AND i.DT_Cadastro >= :ini AND i.DT_Cadastro < :fim " +
                                "  AND i.IDR_UsuarioCadastro IN (SELECT eu2.IDR_Usuario FROM equipe_usuario eu2 " +
                                "    WHERE eu2.IDR_Equipe = e.ID_Equipe AND eu2.FG_Excluido = 0)) coletados, " +
                                "(SELECT COUNT(*) FROM triagem tr JOIN item i2 ON i2.ID_Item = tr.IDR_Item " +
                                "  WHERE i2.IDR_Evento = :ev AND tr.FG_Excluido = 0 AND tr.TP_Status = 'CONCLUIDA' " +
                                "  AND COALESCE(tr.DT_Conclusao, tr.DT_Cadastro) >= :ini " +
                                "  AND COALESCE(tr.DT_Conclusao, tr.DT_Cadastro) < :fim " +
                                "  AND tr.IDR_Operador IN (SELECT eu3.IDR_Usuario FROM equipe_usuario eu3 " +
                                "    WHERE eu3.IDR_Equipe = e.ID_Equipe AND eu3.FG_Excluido = 0)) triados, " +
                                "(SELECT COUNT(*) FROM devolucao d " +
                                "  LEFT JOIN claim_historico h ON h.IDR_Claim = d.IDR_Claim AND h.FG_Excluido = 0 " +
                                "  WHERE d.IDR_Evento = :ev AND d.FG_Excluido = 0 AND d.FG_Concluido = 1 " +
                                "  AND d.DT_Devolucao >= :ini AND d.DT_Devolucao < :fim " +
                                "  AND h.IDR_Operador IN (SELECT eu4.IDR_Usuario FROM equipe_usuario eu4 " +
                                "    WHERE eu4.IDR_Equipe = e.ID_Equipe AND eu4.FG_Excluido = 0)) devolvidos " +
                                "FROM equipe e " +
                                "LEFT JOIN equipe_usuario eu ON eu.IDR_Equipe = e.ID_Equipe AND eu.FG_Excluido = 0 " +
                                "WHERE e.IDR_Evento = :ev AND e.FG_Excluido = 0 AND e.FG_Ativo = 1 " +
                                "GROUP BY e.ID_Equipe, e.NM_Equipe, e.TP_Equipe ORDER BY e.NM_Equipe", Tuple.class)
                .setParameter("ev", ev).setParameter("ini", j.inicio()).setParameter("fim", j.fim())
                .getResultList();

        List<Map<String, Object>> equipeMaps = new ArrayList<>();
        long totalMembros = 0;
        long totalAcoes = 0;
        for (Tuple t : equipes) {
            long membros = PainelChartHelper.asLong(t.get("membros"));
            long coletados = PainelChartHelper.asLong(t.get("coletados"));
            long triados = PainelChartHelper.asLong(t.get("triados"));
            long devolvidos = PainelChartHelper.asLong(t.get("devolvidos"));
            long acoes = coletados + triados + devolvidos;
            totalMembros += membros;
            totalAcoes += acoes;
            double mediaDia = j.dias() > 0 ? PainelChartHelper.round1(acoes / (double) j.dias()) : 0;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("equipe", str(t.get("equipe")));
            m.put("membros", (int) membros);
            m.put("coletados", (int) coletados);
            m.put("triados", (int) triados);
            m.put("devolvidos", (int) devolvidos);
            m.put("mediaDia", mediaDia);
            equipeMaps.add(m);
        }

        @SuppressWarnings("unchecked")
        List<Tuple> ops = em.createNativeQuery(
                        "SELECT u.NM_Usuario operador, COALESCE(e.NM_Equipe, 'Sem equipe') equipe, " +
                                "(SELECT COUNT(*) FROM item i WHERE i.IDR_Evento = :ev AND i.FG_Excluido = 0 " +
                                "  AND i.IDR_UsuarioCadastro = u.ID_Usuario AND i.DT_Cadastro >= :ini AND i.DT_Cadastro < :fim) coletados, " +
                                "(SELECT COUNT(*) FROM triagem tr JOIN item i2 ON i2.ID_Item = tr.IDR_Item " +
                                "  WHERE i2.IDR_Evento = :ev AND tr.FG_Excluido = 0 AND tr.IDR_Operador = u.ID_Usuario " +
                                "  AND tr.TP_Status = 'CONCLUIDA' AND COALESCE(tr.DT_Conclusao, tr.DT_Cadastro) >= :ini " +
                                "  AND COALESCE(tr.DT_Conclusao, tr.DT_Cadastro) < :fim) triados, " +
                                "(SELECT COUNT(*) FROM devolucao d JOIN claim_historico h ON h.IDR_Claim = d.IDR_Claim " +
                                "  WHERE d.IDR_Evento = :ev AND d.FG_Excluido = 0 AND d.FG_Concluido = 1 " +
                                "  AND h.IDR_Operador = u.ID_Usuario AND h.FG_Excluido = 0 " +
                                "  AND d.DT_Devolucao >= :ini AND d.DT_Devolucao < :fim) devolvidos, " +
                                "(SELECT COUNT(*) FROM transferencia t WHERE t.IDR_Evento = :ev AND t.FG_Excluido = 0 " +
                                "  AND t.IDR_UsuarioResponsavel = u.ID_Usuario " +
                                "  AND t.DT_Transferencia >= :ini AND t.DT_Transferencia < :fim) transferencias " +
                                "FROM usuario u " +
                                "JOIN equipe_usuario eu ON eu.IDR_Usuario = u.ID_Usuario AND eu.FG_Excluido = 0 " +
                                "JOIN equipe e ON e.ID_Equipe = eu.IDR_Equipe AND e.IDR_Evento = :ev AND e.FG_Excluido = 0 " +
                                "WHERE u.FG_Excluido = 0 " +
                                "GROUP BY u.ID_Usuario, u.NM_Usuario, e.NM_Equipe " +
                                "ORDER BY (coletados + triados + devolvidos + transferencias) DESC LIMIT 50", Tuple.class)
                .setParameter("ev", ev).setParameter("ini", j.inicio()).setParameter("fim", j.fim())
                .getResultList();

        List<Map<String, Object>> opMaps = new ArrayList<>();
        List<Map.Entry<String, Number>> barraEntries = new ArrayList<>();
        for (Tuple t : ops) {
            int coletados = (int) PainelChartHelper.asLong(t.get("coletados"));
            int triados = (int) PainelChartHelper.asLong(t.get("triados"));
            int devolvidos = (int) PainelChartHelper.asLong(t.get("devolvidos"));
            int transferencias = (int) PainelChartHelper.asLong(t.get("transferencias"));
            int tot = coletados + triados + devolvidos + transferencias;
            if (tot == 0) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("operador", str(t.get("operador")));
            m.put("equipe", str(t.get("equipe")));
            m.put("coletados", coletados);
            m.put("triados", triados);
            m.put("devolvidos", devolvidos);
            m.put("transferencias", transferencias);
            m.put("total", tot);
            opMaps.add(m);
            barraEntries.add(entry(str(t.get("operador")), tot));
        }

        long operadores = opMaps.size();
        double mediaOp = operadores > 0 ? PainelChartHelper.round1(totalAcoes / (double) operadores) : 0;

        Map<String, Object> out = base(j);
        out.put("kpis", List.of(
                PainelChartHelper.kpi("Equipes ativas", String.valueOf(equipeMaps.size()),
                        "bg-indigo-50 dark:bg-indigo-500/20", "text-indigo-500"),
                PainelChartHelper.kpi("Operadores", String.valueOf(Math.max(operadores, totalMembros)),
                        "bg-purple-50 dark:bg-purple-500/20", "text-purple-500"),
                PainelChartHelper.kpi("Ações no período", String.valueOf(totalAcoes),
                        "bg-emerald-50 dark:bg-emerald-500/20", "text-emerald-500"),
                PainelChartHelper.kpi("Média / operador", PainelChartHelper.fmtDecimalBr(mediaOp),
                        "bg-amber-50 dark:bg-amber-500/20", "text-amber-500")));
        out.put("equipes", equipeMaps);
        out.put("operadores", opMaps);
        out.put("barras", PainelChartHelper.bars(barraEntries));
        return out;
    }

    private Map<String, Object> painelAuditoria(Long ev, PainelChartHelper.Janela j) {
        String filtroEvento =
                "( (a.NM_Tabela = 'item' AND EXISTS (SELECT 1 FROM item x WHERE x.ID_Item = a.ID_Registro AND x.IDR_Evento = :ev)) " +
                        "OR (a.NM_Tabela = 'claim' AND EXISTS (SELECT 1 FROM claim x WHERE x.ID_Claim = a.ID_Registro AND x.IDR_Evento = :ev)) " +
                        "OR (a.NM_Tabela = 'devolucao' AND EXISTS (SELECT 1 FROM devolucao x WHERE x.ID_Devolucao = a.ID_Registro AND x.IDR_Evento = :ev)) " +
                        "OR (a.NM_Tabela = 'transferencia' AND EXISTS (SELECT 1 FROM transferencia x WHERE x.ID_Transferencia = a.ID_Registro AND x.IDR_Evento = :ev)) " +
                        "OR (a.NM_Tabela = 'local' AND EXISTS (SELECT 1 FROM local x WHERE x.ID_Local = a.ID_Registro AND x.IDR_Evento = :ev)) " +
                        "OR (a.NM_Tabela = 'equipe' AND EXISTS (SELECT 1 FROM equipe x WHERE x.ID_Equipe = a.ID_Registro AND x.IDR_Evento = :ev)) " +
                        "OR (a.NM_Tabela = 'triagem' AND EXISTS (SELECT 1 FROM triagem t JOIN item i ON i.ID_Item = t.IDR_Item WHERE t.ID_Triagem = a.ID_Registro AND i.IDR_Evento = :ev)) " +
                        "OR (a.NM_Tabela = 'deposito' AND EXISTS (SELECT 1 FROM deposito x WHERE x.ID_Deposito = a.ID_Registro AND x.IDR_Evento = :ev)) )";

        Map<String, Long> porAcao = contarMap(
                "SELECT COALESCE(a.TP_Acao, '—') n, COUNT(*) q FROM auditoria a " +
                        "WHERE a.FG_Excluido = 0 AND a.DT_Auditoria >= :ini AND a.DT_Auditoria < :fim AND " + filtroEvento + " " +
                        "GROUP BY a.TP_Acao ORDER BY q DESC",
                ev, j.inicio(), j.fim());

        long logins = count(
                "SELECT COUNT(*) FROM login_log ll " +
                        "JOIN equipe_usuario eu ON eu.IDR_Usuario = ll.IDR_Usuario AND eu.FG_Excluido = 0 " +
                        "JOIN equipe e ON e.ID_Equipe = eu.IDR_Equipe AND e.IDR_Evento = :ev AND e.FG_Excluido = 0 " +
                        "WHERE ll.FG_Excluido = 0 AND ll.DT_Login >= :ini AND ll.DT_Login < :fim",
                ev, j.inicio(), j.fim());
        if (logins > 0) porAcao.merge("LOGIN", logins, Long::sum);

        long total = porAcao.values().stream().mapToLong(Long::longValue).sum();
        long updates = porAcao.getOrDefault("UPDATE", 0L);
        long inserts = porAcao.getOrDefault("INSERT", 0L);
        long deletes = porAcao.getOrDefault("DELETE", 0L);

        @SuppressWarnings("unchecked")
        List<Tuple> rows = em.createNativeQuery(
                        "SELECT COALESCE(u.NM_Login, u.NM_Usuario, 'sistema') usuario, a.TP_Acao acao, a.NM_Tabela tabela, " +
                                "a.ID_Registro registro, a.DT_Auditoria dataHora, COALESCE(a.NR_IP, '—') ip, " +
                                "COALESCE(CONCAT(ll.NM_Navegador, ' · ', ll.NM_Dispositivo), 'Não informado') dispositivo " +
                                "FROM auditoria a " +
                                "LEFT JOIN usuario u ON u.ID_Usuario = a.IDR_Usuario " +
                                "LEFT JOIN ( " +
                                "  SELECT ll1.IDR_Usuario, ll1.NM_Navegador, ll1.NM_Dispositivo FROM login_log ll1 " +
                                "  INNER JOIN (SELECT IDR_Usuario, MAX(ID_Login) mx FROM login_log WHERE FG_Excluido = 0 GROUP BY IDR_Usuario) x " +
                                "    ON x.mx = ll1.ID_Login " +
                                ") ll ON ll.IDR_Usuario = a.IDR_Usuario " +
                                "WHERE a.FG_Excluido = 0 AND a.DT_Auditoria >= :ini AND a.DT_Auditoria < :fim AND " + filtroEvento + " " +
                                "ORDER BY a.DT_Auditoria DESC LIMIT " + ROW_LIMIT, Tuple.class)
                .setParameter("ev", ev).setParameter("ini", j.inicio()).setParameter("fim", j.fim())
                .getResultList();

        List<Map<String, Object>> rowMaps = new ArrayList<>();
        for (Tuple t : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("usuario", str(t.get("usuario")));
            m.put("acao", str(t.get("acao")));
            m.put("modulo", PainelChartHelper.moduloAuditoria(str(t.get("tabela"))));
            m.put("registro", str(t.get("tabela")) + " · #" + str(t.get("registro")));
            m.put("dataHora", PainelChartHelper.fmtSec(toLdt(t.get("dataHora"))));
            m.put("dispositivo", str(t.get("dispositivo")));
            m.put("ip", PainelChartHelper.nvlDash(str(t.get("ip"))));
            rowMaps.add(m);
        }

        Map<String, Object> out = base(j);
        out.put("kpis", List.of(
                PainelChartHelper.kpi("Eventos registrados", String.valueOf(total),
                        "bg-indigo-50 dark:bg-indigo-500/20", "text-indigo-500"),
                PainelChartHelper.kpi("Updates", String.valueOf(updates),
                        "bg-amber-50 dark:bg-amber-500/20", "text-amber-500"),
                PainelChartHelper.kpi("Inserts", String.valueOf(inserts),
                        "bg-emerald-50 dark:bg-emerald-500/20", "text-emerald-500"),
                PainelChartHelper.kpi("Deletes", String.valueOf(deletes),
                        "bg-red-50 dark:bg-red-500/20", "text-red-500")));
        out.put("acao", PainelChartHelper.slices(toNumberEntries(porAcao)));
        out.put("rows", rowMaps);
        return out;
    }

    private Map<String, Object> painelLgpd(Long ev, PainelChartHelper.Janela j, Evento evento) {
        int retencao = evento.getQtDiasRetencao() != null ? evento.getQtDiasRetencao() : 90;

        @SuppressWarnings("unchecked")
        List<Tuple> itemRows = em.createNativeQuery(
                        "SELECT i.CD_Item codigo, i.NM_Titulo item, i.FG_Sensivel sensivel, s.NM_Status status, " +
                                "COALESCE(c.NM_Categoria, 'Não informado') categoria " +
                                "FROM item i JOIN status_item s ON s.ID_Status = i.IDR_Status " +
                                "LEFT JOIN categoria c ON c.ID_Categoria = i.IDR_Categoria " +
                                "WHERE i.IDR_Evento = :ev AND i.FG_Excluido = 0 " +
                                "AND (i.FG_Sensivel = 1 OR LOWER(COALESCE(c.NM_Categoria,'')) LIKE '%documento%' " +
                                "  OR LOWER(COALESCE(c.NM_Categoria,'')) LIKE '%identidade%' " +
                                "  OR LOWER(COALESCE(i.NM_Titulo,'')) LIKE '%rg%' " +
                                "  OR LOWER(COALESCE(i.NM_Titulo,'')) LIKE '%cpf%' " +
                                "  OR LOWER(COALESCE(i.NM_Titulo,'')) LIKE '%passaporte%' " +
                                "  OR LOWER(COALESCE(i.NM_Titulo,'')) LIKE '%cnh%') " +
                                "AND i.DT_Cadastro >= :ini AND i.DT_Cadastro < :fim " +
                                "ORDER BY i.DT_Cadastro DESC LIMIT 100", Tuple.class)
                .setParameter("ev", ev).setParameter("ini", j.inicio()).setParameter("fim", j.fim())
                .getResultList();

        @SuppressWarnings("unchecked")
        List<Tuple> claimRows = em.createNativeQuery(
                        "SELECT COALESCE(c.CD_Claim, CONCAT('CLM-', YEAR(c.DT_Cadastro), '-', LPAD(c.ID_Claim, 5, '0'))) codigo, " +
                                "CONCAT('Pedido · ', c.NM_Objeto) item, c.NM_Nome titular, s.NM_Status status, " +
                                "c.NR_CPF cpf, c.NM_Email email, c.NR_Telefone telefone " +
                                "FROM claim c JOIN status_item s ON s.ID_Status = c.IDR_Status " +
                                "WHERE c.IDR_Evento = :ev AND c.FG_Excluido = 0 " +
                                "AND (c.NR_CPF IS NOT NULL OR c.NM_Email IS NOT NULL OR c.NR_Telefone IS NOT NULL) " +
                                "AND c.DT_Cadastro >= :ini AND c.DT_Cadastro < :fim " +
                                "ORDER BY c.DT_Cadastro DESC LIMIT 100", Tuple.class)
                .setParameter("ev", ev).setParameter("ini", j.inicio()).setParameter("fim", j.fim())
                .getResultList();

        @SuppressWarnings("unchecked")
        List<Tuple> devRows = em.createNativeQuery(
                        "SELECT CONCAT('DEV-', YEAR(d.DT_Devolucao), '-', LPAD(d.ID_Devolucao, 5, '0')) codigo, " +
                                "CONCAT('Termo · ', i.NM_Titulo) item, d.NM_Recebedor titular, d.FG_Assinado assinado, d.NR_CPF cpf " +
                                "FROM devolucao d JOIN item i ON i.ID_Item = d.IDR_Item " +
                                "WHERE d.IDR_Evento = :ev AND d.FG_Excluido = 0 AND d.FG_Concluido = 1 " +
                                "AND (d.NR_CPF IS NOT NULL OR d.FG_Assinado = 1) " +
                                "AND d.DT_Devolucao >= :ini AND d.DT_Devolucao < :fim " +
                                "ORDER BY d.DT_Devolucao DESC LIMIT 100", Tuple.class)
                .setParameter("ev", ev).setParameter("ini", j.inicio()).setParameter("fim", j.fim())
                .getResultList();

        List<Map<String, Object>> rowMaps = new ArrayList<>();
        Map<String, Long> tipoCount = new LinkedHashMap<>();
        long sensiveis = 0;
        long custodia = 0;
        long arquivados = 0;

        for (Tuple t : itemRows) {
            boolean sensivel = Boolean.TRUE.equals(t.get("sensivel"))
                    || (t.get("sensivel") instanceof Number n && n.intValue() == 1);
            String categoria = str(t.get("categoria"));
            String tipoDado = sensivel ? "Possível mídia com dados" : "Documento de identidade";
            if (categoria != null && categoria.toLowerCase(Locale.ROOT).contains("documento")) {
                tipoDado = "Documento de identidade";
            }
            String statusItem = str(t.get("status"));
            String statusLgpd = List.of("Devolvido", "Finalizado", "Descartado").contains(statusItem) ? "Arquivado" : "Em custódia";
            if (sensivel) sensiveis++;
            if ("Em custódia".equals(statusLgpd)) custodia++; else arquivados++;
            tipoCount.merge(tipoDado, 1L, Long::sum);

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("codigo", str(t.get("codigo")));
            m.put("item", str(t.get("item")));
            m.put("tipoDado", tipoDado);
            m.put("sensibilidade", sensivel ? "Sensível" : "Pessoal");
            m.put("titular", "Não identificado");
            m.put("baseLegal", "Legítimo interesse · restituição");
            m.put("retencao", "Até devolução + " + retencao + " dias");
            m.put("status", statusLgpd);
            rowMaps.add(m);
        }

        for (Tuple t : claimRows) {
            List<String> partes = new ArrayList<>();
            if (t.get("cpf") != null) partes.add("CPF");
            if (t.get("email") != null) partes.add("e-mail");
            if (t.get("telefone") != null) partes.add("telefone");
            partes.add(0, "Nome");
            String tipoDado = "Dados de contato";
            tipoCount.merge(tipoDado, 1L, Long::sum);
            String st = PainelChartHelper.statusClaimPainel(str(t.get("status")));
            String statusLgpd = List.of("Aprovado", "Cancelado", "Rejeitado").contains(st) ? "Arquivado" : "Ativo";
            if ("Arquivado".equals(statusLgpd)) arquivados++; else custodia++;

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("codigo", str(t.get("codigo")));
            m.put("item", str(t.get("item")));
            m.put("tipoDado", String.join(", ", partes));
            m.put("sensibilidade", "Pessoal");
            m.put("titular", PainelChartHelper.nvl(str(t.get("titular"))));
            m.put("baseLegal", "Consentimento / legítimo interesse");
            m.put("retencao", "Enquanto pedido ativo + 1 ano");
            m.put("status", statusLgpd);
            rowMaps.add(m);
        }

        for (Tuple t : devRows) {
            boolean assinado = Boolean.TRUE.equals(t.get("assinado"))
                    || (t.get("assinado") instanceof Number n && n.intValue() == 1);
            String tipoDado = assinado ? "Assinatura / termo" : "Dados de contato";
            if (t.get("cpf") != null) tipoDado = assinado ? "CPF, nome, assinatura" : "CPF, nome";
            tipoCount.merge(assinado ? "Assinatura / termo" : "Dados de contato", 1L, Long::sum);
            arquivados++;

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("codigo", str(t.get("codigo")));
            m.put("item", str(t.get("item")));
            m.put("tipoDado", tipoDado);
            m.put("sensibilidade", "Pessoal");
            m.put("titular", PainelChartHelper.nvl(str(t.get("titular"))));
            m.put("baseLegal", "Execução de contrato / obrigação legal");
            m.put("retencao", "5 anos (fiscal/contábil)");
            m.put("status", "Arquivado");
            rowMaps.add(m);
        }

        List<Map.Entry<String, Number>> tipoEntries = toNumberEntries(tipoCount);
        long totalPii = rowMaps.size();

        Map<String, Object> out = base(j);
        out.put("kpis", List.of(
                PainelChartHelper.kpi("Registros com PII", String.valueOf(totalPii),
                        "bg-indigo-50 dark:bg-indigo-500/20", "text-indigo-500"),
                PainelChartHelper.kpi("Dados sensíveis", String.valueOf(sensiveis),
                        "bg-red-50 dark:bg-red-500/20", "text-red-500"),
                PainelChartHelper.kpi("Em custódia", String.valueOf(custodia),
                        "bg-amber-50 dark:bg-amber-500/20", "text-amber-500"),
                PainelChartHelper.kpi("Arquivados", String.valueOf(arquivados),
                        "bg-emerald-50 dark:bg-emerald-500/20", "text-emerald-500")));
        out.put("tipo", PainelChartHelper.slices(tipoEntries));
        out.put("rows", rowMaps.size() > ROW_LIMIT ? rowMaps.subList(0, ROW_LIMIT) : rowMaps);
        return out;
    }

    // ------------------------------------------------------------------
    // Endpoints legados (views)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<Map<String, Object>> itensPorCategoria(String idEvento) {
        return porEvento("SELECT * FROM VW_Itens_Categoria WHERE ID_Evento = :ev ORDER BY QT_Itens DESC", idEvento);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> itensPendentes(String idEvento) {
        return porEvento("SELECT * FROM VW_Itens_Pendentes WHERE ID_Evento = :ev ORDER BY QT_DiasArmazenado DESC", idEvento);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> tempoDevolucao(String idEvento) {
        return porEvento("SELECT * FROM VW_Tempo_Devolucao WHERE ID_Evento = :ev", idEvento);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> auditoria(String idEvento) {
        return porEvento("SELECT * FROM VW_Auditoria_Evento WHERE ID_Evento = :ev ORDER BY DT_Auditoria DESC", idEvento);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> itensDevolvidos(String idEvento) {
        return porNomeEvento("SELECT * FROM VW_Itens_Devolvidos WHERE NM_Evento = :ev ORDER BY DT_Devolucao DESC", idEvento);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> claimsAbertos(String idEvento) {
        return porNomeEvento("SELECT * FROM VW_Claims_Abertos WHERE NM_Evento = :ev ORDER BY QT_DiasAberto DESC", idEvento);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> slaEstourado(String idEvento) {
        return porNomeEvento("SELECT * FROM VW_Sla_Estourado WHERE NM_Evento = :ev ORDER BY QT_HorasEstouradas DESC", idEvento);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> itensPorLocalizacao() {
        return executar(em.createNativeQuery(
                "SELECT * FROM VW_Itens_Localizacao ORDER BY NM_Deposito, NM_Setor", Tuple.class).getResultList());
    }

    // ------------------------------------------------------------------

    private Map<String, Object> base(PainelChartHelper.Janela j) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("dias", j.dias());
        out.put("inicio", j.inicio().toString());
        out.put("fim", j.fim().toString());
        return out;
    }

    private List<Map<String, Object>> porEvento(String sql, String idEvento) {
        Long ev = idCodec.decodeEventoId(idEvento);
        return executar(em.createNativeQuery(sql, Tuple.class).setParameter("ev", ev).getResultList());
    }

    private List<Map<String, Object>> porNomeEvento(String sql, String idEvento) {
        Evento evento = eventoRepository.findById(idCodec.decodeEventoId(idEvento))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento não encontrado."));
        return executar(em.createNativeQuery(sql, Tuple.class).setParameter("ev", evento.getNmEvento()).getResultList());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> executar(List<?> rows) {
        return ((List<Tuple>) rows).stream().map(this::toMap).toList();
    }

    private Map<String, Object> toMap(Tuple t) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (TupleElement<?> e : t.getElements()) {
            m.put(e.getAlias(), t.get(e.getAlias()));
        }
        return m;
    }

    private long count(String sql, Long ev, LocalDateTime ini, LocalDateTime fim) {
        Number n = (Number) em.createNativeQuery(sql)
                .setParameter("ev", ev).setParameter("ini", ini).setParameter("fim", fim)
                .getSingleResult();
        return n == null ? 0L : n.longValue();
    }

    private Map<String, Long> contarMap(String sql, Long ev, LocalDateTime ini, LocalDateTime fim) {
        @SuppressWarnings("unchecked")
        List<Tuple> rows = em.createNativeQuery(sql, Tuple.class)
                .setParameter("ev", ev).setParameter("ini", ini).setParameter("fim", fim)
                .getResultList();
        Map<String, Long> map = new LinkedHashMap<>();
        for (Tuple t : rows) {
            map.put(str(t.get("n")), PainelChartHelper.asLong(t.get("q")));
        }
        return map;
    }

    private List<Map.Entry<String, Number>> entriesFrom(String sql, Long ev, LocalDateTime ini, LocalDateTime fim) {
        @SuppressWarnings("unchecked")
        List<Tuple> rows = em.createNativeQuery(sql, Tuple.class)
                .setParameter("ev", ev).setParameter("ini", ini).setParameter("fim", fim)
                .getResultList();
        List<Map.Entry<String, Number>> list = new ArrayList<>();
        for (Tuple t : rows) {
            list.add(entry(str(t.get("n")), PainelChartHelper.asLong(t.get("q"))));
        }
        return list;
    }

    private List<Map.Entry<String, Number>> entriesFromNoPeriod(String sql, Long ev) {
        @SuppressWarnings("unchecked")
        List<Tuple> rows = em.createNativeQuery(sql, Tuple.class).setParameter("ev", ev).getResultList();
        List<Map.Entry<String, Number>> list = new ArrayList<>();
        for (Tuple t : rows) {
            list.add(entry(str(t.get("n")), PainelChartHelper.asLong(t.get("q"))));
        }
        return list;
    }

    private static Map.Entry<String, Number> entry(String k, Number v) {
        return new AbstractMap.SimpleEntry<>(k, v);
    }

    private static List<Map.Entry<String, Number>> toNumberEntries(Map<String, Long> map) {
        List<Map.Entry<String, Number>> list = new ArrayList<>();
        for (Map.Entry<String, Long> e : map.entrySet()) {
            list.add(entry(e.getKey(), e.getValue()));
        }
        return list;
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static LocalDateTime toLdt(Object o) {
        if (o == null) return null;
        if (o instanceof LocalDateTime ldt) return ldt;
        if (o instanceof java.sql.Timestamp ts) return ts.toLocalDateTime();
        if (o instanceof java.sql.Date d) return d.toLocalDate().atStartOfDay();
        return LocalDateTime.parse(o.toString().replace(' ', 'T').substring(0, Math.min(19, o.toString().length())));
    }
}
