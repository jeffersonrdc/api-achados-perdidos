-- Índice para listagem de matches pendentes por claim.

SET @schema_name = DATABASE();

SET @sql = IF(
  EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'claim_validacao'
      AND INDEX_NAME = 'IX_claimvalidacao_claim_resultado'
  ),
  'SELECT ''IX_claimvalidacao_claim_resultado ja existe'' AS info',
  'ALTER TABLE claim_validacao ADD INDEX IX_claimvalidacao_claim_resultado (IDR_Claim, ST_Resultado, FG_Excluido)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
