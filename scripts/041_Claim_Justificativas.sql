-- Justificativas obrigatórias das decisões de aprovação e reprovação do pedido.
-- Os valores também continuam registrados em claim_historico para auditoria.

SET @schema_name = DATABASE();

SET @sql = IF(
  EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'claim'
      AND COLUMN_NAME = 'DS_JustificativaAprovacao'
  ),
  'SELECT ''DS_JustificativaAprovacao ja existe'' AS info',
  'ALTER TABLE claim ADD COLUMN DS_JustificativaAprovacao VARCHAR(1000) NULL AFTER DS_Tags'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'claim'
      AND COLUMN_NAME = 'DS_JustificativaReprovacao'
  ),
  'SELECT ''DS_JustificativaReprovacao ja existe'' AS info',
  'ALTER TABLE claim ADD COLUMN DS_JustificativaReprovacao VARCHAR(1000) NULL AFTER DS_JustificativaAprovacao'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
