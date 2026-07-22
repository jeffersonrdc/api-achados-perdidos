-- Status de match para claims PERDA (itens perdidos).
-- Idempotente.

INSERT INTO status_item (NM_Status, DS_Status, OR_Ordem, FG_Final, FG_Ativo, FG_Excluido, DT_Cadastro)
SELECT 'Aguardando Match', 'Relato finalizado — buscando correspondência na coleta', 91, 0, 1, 0, NOW()
WHERE NOT EXISTS (SELECT 1 FROM status_item WHERE NM_Status = 'Aguardando Match');

INSERT INTO status_item (NM_Status, DS_Status, OR_Ordem, FG_Final, FG_Ativo, FG_Excluido, DT_Cadastro)
SELECT 'Match', 'Há candidato(s) da coleta com similaridade suficiente', 92, 0, 1, 0, NOW()
WHERE NOT EXISTS (SELECT 1 FROM status_item WHERE NM_Status = 'Match');

-- Migra claims PERDA ainda em "Claim Aberto" para o novo fluxo.
UPDATE claim c
INNER JOIN status_item s_old ON s_old.ID_Status = c.IDR_Status AND s_old.NM_Status = 'Claim Aberto'
INNER JOIN status_item s_new ON s_new.NM_Status = 'Match' AND s_new.FG_Excluido = 0
SET c.IDR_Status = s_new.ID_Status
WHERE c.FG_Excluido = 0
  AND UPPER(IFNULL(c.TP_Claim, 'PERDA')) = 'PERDA'
  AND EXISTS (
    SELECT 1 FROM claim_validacao v
    WHERE v.IDR_Claim = c.ID_Claim
      AND v.FG_Excluido = 0
      AND v.ST_Resultado = 'PENDENTE'
  );

UPDATE claim c
INNER JOIN status_item s_old ON s_old.ID_Status = c.IDR_Status AND s_old.NM_Status = 'Claim Aberto'
INNER JOIN status_item s_new ON s_new.NM_Status = 'Aguardando Match' AND s_new.FG_Excluido = 0
SET c.IDR_Status = s_new.ID_Status
WHERE c.FG_Excluido = 0
  AND UPPER(IFNULL(c.TP_Claim, 'PERDA')) = 'PERDA'
  AND NOT EXISTS (
    SELECT 1 FROM claim_validacao v
    WHERE v.IDR_Claim = c.ID_Claim
      AND v.FG_Excluido = 0
      AND v.ST_Resultado = 'PENDENTE'
  );
