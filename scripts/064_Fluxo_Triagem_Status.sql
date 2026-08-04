-- =====================================================================
-- 064_Fluxo_Triagem_Status.sql
-- Realinha os itens legados ao fluxo de triagem:
--   cadastro (/itens)        -> "Aguardando triagem"
--   "Analisar item"          -> "Em triagem"
--   "Concluir triagem"       -> "Em estoque"  (visível no portal público)
--
-- Antes desta correção o cadastro gravava "Em estoque" direto, deixando
-- itens sem triagem visíveis no portal. Este script devolve à fila os
-- itens que ainda não tiveram a triagem concluída.
--
-- ATENÇÃO: altera dados. Faça backup e rode em transação/homologação antes.
-- Aplicar com: mysql --default-character-set=utf8mb4
-- Idempotente.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) Confira o impacto antes de aplicar (nenhuma alteração aqui).
-- ---------------------------------------------------------------------
-- SELECT i.ID_Item, i.CD_Item, i.NM_Titulo, s.NM_Status, i.DT_Cadastro
--   FROM item i
--   INNER JOIN status_item s ON s.ID_Status = i.IDR_Status
--  WHERE i.FG_Excluido = 0
--    AND s.NM_Status = 'Em estoque'
--    AND NOT EXISTS (SELECT 1 FROM triagem t
--                     WHERE t.IDR_Item = i.ID_Item
--                       AND t.FG_Excluido = 0
--                       AND t.TP_Status = 'CONCLUIDA');

-- ---------------------------------------------------------------------
-- 2) "Em Análise" (status legado do antigo botão "Analisar item")
--    passa a ser "Em triagem".
-- ---------------------------------------------------------------------
UPDATE item i
INNER JOIN status_item s_old ON s_old.ID_Status = i.IDR_Status AND s_old.NM_Status = 'Em Análise'
INNER JOIN status_item s_new ON s_new.NM_Status = 'Em triagem' AND s_new.FG_Excluido = 0
SET i.IDR_Status = s_new.ID_Status,
    i.DT_Alteracao = NOW()
WHERE i.FG_Excluido = 0;

-- ---------------------------------------------------------------------
-- 3) Registra na linha do tempo os itens que voltarão à fila.
--    Precisa rodar ANTES do UPDATE do passo 4, enquanto ainda dá para
--    identificar quais itens serão afetados.
-- ---------------------------------------------------------------------
INSERT INTO item_historico (IDR_Item, IDR_StatusAnterior, IDR_StatusNovo, DS_Historico, DT_Historico, DT_Cadastro, FG_Excluido)
SELECT i.ID_Item,
       i.IDR_Status,
       (SELECT ID_Status FROM status_item WHERE NM_Status = 'Aguardando triagem' AND FG_Excluido = 0 LIMIT 1),
       'Status realinhado ao fluxo de triagem (script 064): item retornou à fila por não ter triagem concluída.',
       NOW(), NOW(), 0
  FROM item i
 INNER JOIN status_item s ON s.ID_Status = i.IDR_Status AND s.NM_Status = 'Em estoque'
 WHERE i.FG_Excluido = 0
   AND i.FG_Entregue = 0
   AND i.FG_Descartado = 0
   AND NOT EXISTS (
     SELECT 1 FROM triagem t
      WHERE t.IDR_Item = i.ID_Item
        AND t.FG_Excluido = 0
        AND t.TP_Status = 'CONCLUIDA'
   )
   AND NOT EXISTS (
     SELECT 1 FROM item_historico h
      WHERE h.IDR_Item = i.ID_Item
        AND h.DS_Historico LIKE 'Status realinhado ao fluxo de triagem (script 064)%'
   );

-- ---------------------------------------------------------------------
-- 4) Itens "Em estoque" sem triagem concluída voltam para a fila.
--    Preserva itens já comprometidos com devolução/retirada e os que
--    tiveram a triagem concluída.
-- ---------------------------------------------------------------------
UPDATE item i
INNER JOIN status_item s_old ON s_old.ID_Status = i.IDR_Status AND s_old.NM_Status = 'Em estoque'
INNER JOIN status_item s_new ON s_new.NM_Status = 'Aguardando triagem' AND s_new.FG_Excluido = 0
SET i.IDR_Status = s_new.ID_Status,
    i.DT_Alteracao = NOW()
WHERE i.FG_Excluido = 0
  AND i.FG_Entregue = 0
  AND i.FG_Descartado = 0
  AND NOT EXISTS (
    SELECT 1 FROM triagem t
     WHERE t.IDR_Item = i.ID_Item
       AND t.FG_Excluido = 0
       AND t.TP_Status = 'CONCLUIDA'
  );
