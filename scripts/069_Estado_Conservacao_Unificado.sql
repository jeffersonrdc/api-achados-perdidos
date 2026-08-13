-- =====================================================================
-- 069_Estado_Conservacao_Unificado.sql
-- Unifica estados de conservação sem ambiguidade.
-- Canônico: Novo → Bom estado → Usado → Danificado
-- Remove do catálogo ativo: Avariado (sobreposição com Danificado).
-- =====================================================================

-- Atualiza / garante os 4 estados canônicos
INSERT INTO estado (NM_Estado, DS_Estado, OR_Ordem, FG_Ativo, FG_Excluido) VALUES
  ('Novo',        'Sem sinais de uso',                          1, 1, 0),
  ('Bom estado',  'Pequenos sinais de uso, plenamente funcional', 2, 1, 0),
  ('Usado',       'Sinais evidentes de uso, funcional',         3, 1, 0),
  ('Danificado',  'Com danos que comprometem o uso',            4, 1, 0)
ON DUPLICATE KEY UPDATE
  DS_Estado = VALUES(DS_Estado),
  OR_Ordem = VALUES(OR_Ordem),
  FG_Ativo = 1,
  FG_Excluido = 0;

-- Desativa opções ambíguas / legadas (mantém histórico em registros antigos)
UPDATE estado
   SET FG_Ativo = 0,
       FG_Excluido = 1,
       DT_Alteracao = NOW()
 WHERE LOWER(NM_Estado) IN ('avariado', 'bom', 'desgastado')
   AND FG_Excluido = 0;
