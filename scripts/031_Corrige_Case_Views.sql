-- =====================================================================
-- 031_Corrige_Case_Views.sql
-- Corrige o CASE (maiúsculas/minúsculas) dos nomes das views.
--
-- Contexto: o código (RelatorioService/AnalyticsService) consulta as views
-- em CamelCase (ex.: VW_Itens_Categoria). No MySQL do Windows
-- (lower_case_table_names=1) o nome é case-insensitive e funciona; no MySQL
-- do Linux (lower_case_table_names=0) o nome é case-SENSITIVE. Após um
-- restore de backup feito no Windows, as views ficam em minúsculo
-- (vw_itens_categoria) e o código quebra com "Table doesn't exist" -> HTTP 500.
--
-- Rode este script APENAS quando as views vierem em minúsculo (ex.: depois de
-- restaurar um dump gerado no Windows em um servidor Linux).
-- =====================================================================
RENAME TABLE
  vw_auditoria_evento  TO VW_Auditoria_Evento,
  vw_claims_abertos    TO VW_Claims_Abertos,
  vw_dashboard_evento  TO VW_Dashboard_Evento,
  vw_itens_categoria   TO VW_Itens_Categoria,
  vw_itens_devolvidos  TO VW_Itens_Devolvidos,
  vw_itens_localizacao TO VW_Itens_Localizacao,
  vw_itens_pendentes   TO VW_Itens_Pendentes,
  vw_sla_estourado     TO VW_Sla_Estourado,
  vw_sla_pendente      TO VW_Sla_Pendente,
  vw_sla_resumo        TO VW_Sla_Resumo,
  vw_tempo_devolucao   TO VW_Tempo_Devolucao;
