package br.com.achadosperdidos.service;

import br.com.achadosperdidos.config.TimeConfig;
import br.com.achadosperdidos.entity.Evento;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Helpers compartilhados pelos painéis de Relatórios e Analytics. */
final class PainelChartHelper {

    static final String[] PALETTE = {
            "#6366f1", "#10b981", "#f59e0b", "#ef4444", "#8b5cf6",
            "#3b82f6", "#ec4899", "#14b8a6", "#f97316", "#94a3b8"
    };

    static final DateTimeFormatter DT_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    static final DateTimeFormatter DT_BR_SEC = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    static final DateTimeFormatter DIA_BR = DateTimeFormatter.ofPattern("EEE d", Locale.forLanguageTag("pt-BR"));

    private PainelChartHelper() {}

    record Janela(LocalDateTime inicio, LocalDateTime fim, LocalDateTime prevInicio, LocalDateTime prevFim, int dias) {}

    static Janela janela(Evento evento, int dias) {
        LocalDateTime fim = TimeConfig.now();
        LocalDateTime inicio;
        if (dias <= 0) {
            inicio = evento.getDtInicio() != null ? evento.getDtInicio() : fim.minusDays(30);
            if (evento.getDtFim() != null && evento.getDtFim().isBefore(fim)) {
                fim = evento.getDtFim();
            }
        } else {
            inicio = fim.minusDays(dias);
        }
        if (inicio.isAfter(fim)) {
            inicio = fim;
        }
        long segundos = Math.max(1L, ChronoUnit.SECONDS.between(inicio, fim));
        LocalDateTime prevFim = inicio;
        LocalDateTime prevInicio = inicio.minusSeconds(segundos);
        int diasEfetivos = dias > 0 ? dias : Math.max(1, (int) ChronoUnit.DAYS.between(inicio.toLocalDate(), fim.toLocalDate()) + 1);
        return new Janela(inicio, fim, prevInicio, prevFim, diasEfetivos);
    }

    static Map<String, Object> kpi(String label, String value, String color, String iconTone) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("label", label);
        m.put("value", value);
        m.put("color", color);
        m.put("iconTone", iconTone);
        return m;
    }

    static Map<String, Object> kpiChange(String label, String value, String change, Boolean positive, String color, String iconTone) {
        Map<String, Object> m = kpi(label, value, color, iconTone);
        if (change != null) m.put("change", change);
        if (positive != null) m.put("positive", positive);
        return m;
    }

    static List<Map<String, Object>> slices(List<? extends Map.Entry<String, ? extends Number>> entries) {
        double total = entries.stream().mapToDouble(e -> e.getValue().doubleValue()).sum();
        if (total <= 0) total = 1;
        List<Map<String, Object>> out = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, ? extends Number> e : entries) {
            long value = e.getValue().longValue();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", e.getKey());
            m.put("value", value);
            m.put("pct", (int) Math.round((value / total) * 100.0));
            m.put("color", PALETTE[i % PALETTE.length]);
            out.add(m);
            i++;
        }
        return out;
    }

    static List<Map<String, Object>> bars(List<? extends Map.Entry<String, ? extends Number>> entries) {
        double max = entries.stream().mapToDouble(e -> e.getValue().doubleValue()).max().orElse(1);
        if (max <= 0) max = 1;
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map.Entry<String, ? extends Number> e : entries) {
            long value = e.getValue().longValue();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", e.getKey());
            m.put("value", value);
            m.put("pct", (int) Math.round((value / max) * 100.0));
            out.add(m);
        }
        return out;
    }

    static List<Map<String, Object>> barsWithExtra(List<Object[]> entries) {
        double max = entries.stream().mapToDouble(e -> ((Number) e[1]).doubleValue()).max().orElse(1);
        if (max <= 0) max = 1;
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object[] e : entries) {
            long value = ((Number) e[1]).longValue();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", String.valueOf(e[0]));
            m.put("value", value);
            m.put("pct", (int) Math.round((value / max) * 100.0));
            if (e.length > 2 && e[2] != null) m.put("extra", String.valueOf(e[2]));
            out.add(m);
        }
        return out;
    }

    static String fmt(LocalDateTime dt) {
        return dt == null ? "—" : DT_BR.format(dt);
    }

    static String fmtSec(LocalDateTime dt) {
        return dt == null ? "—" : DT_BR_SEC.format(dt);
    }

    static String nvl(String v) {
        return (v == null || v.isBlank()) ? "Não informado" : v;
    }

    static String nvlDash(String v) {
        return (v == null || v.isBlank()) ? "—" : v;
    }

    static String maskCpf(String cpf) {
        if (cpf == null || cpf.isBlank()) return "—";
        String d = cpf.replaceAll("\\D", "");
        if (d.length() != 11) return "CPF ***";
        return "CPF ***." + d.substring(3, 6) + "." + d.substring(6, 9) + "-**";
    }

    static String changePct(long atual, long anterior) {
        if (anterior <= 0) {
            if (atual <= 0) return "0% vs período ant.";
            return "+" + atual + " vs período ant.";
        }
        long pct = Math.round(((atual - anterior) * 100.0) / anterior);
        String sign = pct > 0 ? "+" : "";
        return sign + pct + "% vs período ant.";
    }

    static boolean changePositive(long atual, long anterior, boolean menorMelhor) {
        long delta = atual - anterior;
        if (delta == 0) return true;
        return menorMelhor ? delta < 0 : delta > 0;
    }

    static double round1(double v) {
        return BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    static String fmtDecimalBr(double v) {
        return String.format(Locale.forLanguageTag("pt-BR"), "%.1f", v);
    }

    static long asLong(Object o) {
        return o == null ? 0L : ((Number) o).longValue();
    }

    static double asDouble(Object o) {
        return o == null ? 0d : ((Number) o).doubleValue();
    }

    static String statusClaimPainel(String nm) {
        if (nm == null) return "—";
        return switch (nm) {
            case "Claim Aprovado" -> "Aprovado";
            case "Claim Rejeitado" -> "Rejeitado";
            case "Claim Cancelado" -> "Cancelado";
            case "Claim Aguardando Info" -> "Pendência";
            case "Claim em Análise", "Claim Aberto" -> "Em análise";
            default -> nm.startsWith("Claim ") ? nm.substring(6) : nm;
        };
    }

    static String statusTransferenciaPainel(String tp) {
        if (tp == null) return "—";
        return switch (tp.toUpperCase(Locale.ROOT)) {
            case "CONCLUIDA" -> "Concluída";
            case "EM_TRANSITO" -> "Em trânsito";
            case "DIVERGENCIA" -> "Divergência";
            case "CANCELADA" -> "Cancelada";
            default -> tp;
        };
    }

    static String moduloAuditoria(String tabela) {
        if (tabela == null) return "—";
        return switch (tabela.toLowerCase(Locale.ROOT)) {
            case "item" -> "Itens";
            case "claim" -> "Pedidos";
            case "devolucao" -> "Devoluções";
            case "transferencia" -> "Transferências";
            case "triagem" -> "Triagem";
            case "localizacao", "deposito", "estoque_endereco" -> "Estoque";
            case "usuario" -> "Usuários";
            case "equipe", "equipe_usuario" -> "Equipes";
            case "local" -> "Locais";
            case "login_log" -> "Login";
            default -> Character.toUpperCase(tabela.charAt(0)) + tabela.substring(1);
        };
    }
}
