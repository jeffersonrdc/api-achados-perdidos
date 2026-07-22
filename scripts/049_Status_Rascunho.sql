-- Status "Rascunho" para relatos de perda salvos parcialmente no painel.
-- Idempotente.

INSERT INTO status_item (NM_Status, DS_Status, OR_Ordem, FG_Final, FG_Ativo, FG_Excluido, DT_Cadastro)
SELECT 'Rascunho', 'Cadastro parcial — ainda não finalizado pelo operador', 90, 0, 1, 0, NOW()
WHERE NOT EXISTS (SELECT 1 FROM status_item WHERE NM_Status = 'Rascunho');
