-- =====================================================================
-- 030_Status_Em_Analise.sql
-- Novo status do item: "Em Análise" (usado ao clicar em "Analisar Item"
-- na tela /triagem, antes de concluir a triagem).
-- Aplicar com: mysql --default-character-set=utf8mb4
-- Idempotente.
-- =====================================================================
INSERT INTO status_item (NM_Status, DS_Status, OR_Ordem, FG_Final, FG_Ativo, FG_Excluido, DT_Cadastro)
SELECT 'Em Análise', 'Item em análise durante a triagem', 4, 0, 1, 0, NOW()
WHERE NOT EXISTS (SELECT 1 FROM status_item WHERE NM_Status = 'Em Análise');
