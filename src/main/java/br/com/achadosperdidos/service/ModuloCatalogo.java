package br.com.achadosperdidos.service;

/** Catálogo canônico de módulos do painel (path = rota Angular). */
public record ModuloCatalogo(String path, String label, String chaveParametro, boolean bloqueado) {

    public static final ModuloCatalogo[] TODOS = {
            new ModuloCatalogo("dashboard", "Dashboard", "MODULO_DASHBOARD", false),
            new ModuloCatalogo("itens-perdidos", "Comunicações de Perda", "MODULO_ITENS_PERDIDOS", false),
            new ModuloCatalogo("itens", "Registro de Achados", "MODULO_ITENS", false),
            new ModuloCatalogo("triagem", "Triagem", "MODULO_TRIAGEM", false),
            new ModuloCatalogo("estoque", "Estoque", "MODULO_ESTOQUE", false),
            new ModuloCatalogo("transferencias", "Transferências", "MODULO_TRANSFERENCIAS", false),
            new ModuloCatalogo("pedidos", "Pedidos de Devolução", "MODULO_PEDIDOS", false),
            new ModuloCatalogo("devolucao-rapida", "Devolução Rápida", "MODULO_DEVOLUCAO_RAPIDA", false),
            new ModuloCatalogo("devolucoes", "Devoluções", "MODULO_DEVOLUCOES", false),
            new ModuloCatalogo("eventos", "Eventos", "MODULO_EVENTOS", false),
            new ModuloCatalogo("caracteristicas", "Características", "MODULO_CARACTERISTICAS", false),
            new ModuloCatalogo("logistica-fisica", "Logística Física", "MODULO_LOGISTICA_FISICA", false),
            new ModuloCatalogo("locais", "Locais", "MODULO_LOCAIS", false),
            new ModuloCatalogo("usuarios", "Usuários", "MODULO_USUARIOS", false),
            new ModuloCatalogo("equipes", "Equipes", "MODULO_EQUIPES", false),
            new ModuloCatalogo("relatorios", "Relatórios", "MODULO_RELATORIOS", false),
            new ModuloCatalogo("analytics", "Analytics", "MODULO_ANALYTICS", false),
            new ModuloCatalogo("configuracoes", "Configurações", "MODULO_CONFIGURACOES", true),
            new ModuloCatalogo("perfil", "Perfis", "MODULO_PERFIL", true),
            new ModuloCatalogo("permissoes", "Permissões", "MODULO_PERMISSOES", true),
            new ModuloCatalogo("logs", "Logs de Auditoria", "MODULO_LOGS", false),
    };

    public static ModuloCatalogo porPath(String path) {
        if (path == null || path.isBlank()) return null;
        String p = path.trim().toLowerCase();
        for (ModuloCatalogo m : TODOS) {
            if (m.path().equals(p)) return m;
        }
        return null;
    }
}
