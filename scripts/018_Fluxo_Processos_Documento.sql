-- =====================================================================
-- 018_Fluxo_Processos_Documento.sql
-- Alinha o ciclo de vida do item ao fluxo da Especificacao Funcional
-- (Rock in Rio) e adiciona os campos operacionais do item.
--
-- Idempotente para os INSERTs de status. Os ALTER TABLE devem ser
-- executados uma unica vez (MySQL nao suporta ADD COLUMN IF NOT EXISTS).
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) Ciclo de status do item conforme documento (secao 11)
--    Reaproveita os IDs existentes (FK-safe) e insere os que faltam.
--    Os status de claim (IDs 8..14) permanecem inalterados.
-- ---------------------------------------------------------------------
UPDATE status_item SET NM_Status='Encontrado',              DS_Status='Item localizado por equipe, seguranca ou terceiro',       OR_Ordem=1,  FG_Final=0 WHERE ID_Status=1;
UPDATE status_item SET NM_Status='Coletado',                DS_Status='Item cadastrado inicialmente no sistema',                 OR_Ordem=2,  FG_Final=0 WHERE ID_Status=2;
UPDATE status_item SET NM_Status='Em estoque',              DS_Status='Item armazenado e disponivel para consulta/solicitacao',  OR_Ordem=6,  FG_Final=0 WHERE ID_Status=3;
UPDATE status_item SET NM_Status='Com pedido de devolucao', DS_Status='Existe solicitacao de devolucao vinculada ao item',       OR_Ordem=7,  FG_Final=0 WHERE ID_Status=4;
UPDATE status_item SET NM_Status='Aguardando retirada',     DS_Status='Pedido aprovado e item separado para entrega',            OR_Ordem=8,  FG_Final=0 WHERE ID_Status=5;
UPDATE status_item SET NM_Status='Devolvido',               DS_Status='Item entregue ao responsavel',                            OR_Ordem=9,  FG_Final=1 WHERE ID_Status=6;
UPDATE status_item SET NM_Status='Descartado',              DS_Status='Item descartado conforme politica',                       OR_Ordem=11, FG_Final=1 WHERE ID_Status=7;

INSERT INTO status_item (NM_Status, DS_Status, OR_Ordem, FG_Final, FG_Ativo, FG_Excluido)
SELECT 'Aguardando triagem', 'Item etiquetado e aguardando validacao', 3, 0, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM status_item WHERE NM_Status = 'Aguardando triagem');

INSERT INTO status_item (NM_Status, DS_Status, OR_Ordem, FG_Final, FG_Ativo, FG_Excluido)
SELECT 'Em triagem', 'Item em conferencia e classificacao', 4, 0, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM status_item WHERE NM_Status = 'Em triagem');

INSERT INTO status_item (NM_Status, DS_Status, OR_Ordem, FG_Final, FG_Ativo, FG_Excluido)
SELECT 'Em transporte para estoque', 'Item transferido para armazenamento', 5, 0, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM status_item WHERE NM_Status = 'Em transporte para estoque');

INSERT INTO status_item (NM_Status, DS_Status, OR_Ordem, FG_Final, FG_Ativo, FG_Excluido)
SELECT 'Finalizado', 'Processo arquivado com historico e auditoria', 10, 1, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM status_item WHERE NM_Status = 'Finalizado');

-- ---------------------------------------------------------------------
-- 2) Campos operacionais do item (secoes 4 e 6 do documento)
--    - TP_Prioridade : ALTA | MEDIA | BAIXA
--    - FG_Sensivel   : item sensivel (documento, cartao, dinheiro, etc.)
--    - IDR_Subcategoria : subcategoria (referencia categoria)
-- ---------------------------------------------------------------------
ALTER TABLE item
  ADD COLUMN TP_Prioridade    VARCHAR(10)     NULL              AFTER NM_Serie,
  ADD COLUMN FG_Sensivel      TINYINT(1)      NOT NULL DEFAULT 0 AFTER FG_Perigoso,
  ADD COLUMN IDR_Subcategoria BIGINT UNSIGNED NULL              AFTER IDR_Categoria;

ALTER TABLE item
  ADD CONSTRAINT FK_item_subcategoria FOREIGN KEY (IDR_Subcategoria) REFERENCES categoria (ID_Categoria);

CREATE INDEX IX_item_prioridade ON item (TP_Prioridade);
CREATE INDEX IX_item_sensivel   ON item (FG_Sensivel);

-- ---------------------------------------------------------------------
-- 3) Timeline de status passa a ser controlada pela aplicacao
--    (WorkflowService). Estes dois gatilhos gravavam item_historico de
--    forma generica e agora causariam registros duplicados. Os gatilhos
--    de AUDITORIA (TRG_item_ai_audit / TRG_item_au_audit -> tabela
--    auditoria) sao mantidos.
--
--    Obs.: a linha do tempo passa a existir tambem em ambientes sem
--    triggers (schema gerado por Hibernate nos testes), tornando o
--    comportamento portavel entre producao e testes.
-- ---------------------------------------------------------------------
DROP TRIGGER IF EXISTS TRG_item_ai_auditoria;
DROP TRIGGER IF EXISTS TRG_item_au_status;
