-- =====================================================================
-- 061a_Fix_UK_devolucao_claim.sql
-- Correção pontual quando 061 falha com:
--   Error 1062: Duplicate entry 'N' for key 'devolucao.UK_devolucao_claim'
-- Mantém 1 ticket por claim (ativo mais recente) e cria a UNIQUE.
-- =====================================================================

-- Ver duplicatas (opcional)
-- SELECT IDR_Claim, COUNT(*) qt, GROUP_CONCAT(ID_Devolucao) ids
-- FROM devolucao WHERE IDR_Claim IS NOT NULL
-- GROUP BY IDR_Claim HAVING COUNT(*) > 1;

UPDATE devolucao d
INNER JOIN (
  SELECT
    IDR_Claim,
    COALESCE(
      MAX(CASE WHEN COALESCE(FG_Excluido, 0) = 0 THEN ID_Devolucao END),
      MAX(ID_Devolucao)
    ) AS keep_id
  FROM devolucao
  WHERE IDR_Claim IS NOT NULL
  GROUP BY IDR_Claim
  HAVING COUNT(*) > 1
) k ON k.IDR_Claim = d.IDR_Claim AND d.ID_Devolucao <> k.keep_id
SET
  d.FG_Excluido = 1,
  d.FG_Ativo = 0,
  d.IDR_Claim = NULL;

-- Só cria se ainda não existir
SET @schema_name = DATABASE();
SET @sql = IF(
  EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'devolucao'
      AND INDEX_NAME = 'UK_devolucao_claim'
  ),
  'SELECT ''UK_devolucao_claim ja existe'' AS info',
  'ALTER TABLE devolucao ADD UNIQUE KEY UK_devolucao_claim (IDR_Claim)'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT 'UK_devolucao_claim ok' AS info;
