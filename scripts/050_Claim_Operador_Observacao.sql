-- Operador responsável e observação interna no claim (painel: itens perdidos).

SET @schema_name = DATABASE();

SET @sql = IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'claim' AND COLUMN_NAME = 'NM_Operador'
  ),
  'SELECT ''NM_Operador ja existe'' AS info',
  'ALTER TABLE claim ADD COLUMN NM_Operador VARCHAR(150) NULL AFTER NM_Local'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'claim' AND COLUMN_NAME = 'DS_Observacao'
  ),
  'SELECT ''DS_Observacao ja existe'' AS info',
  'ALTER TABLE claim ADD COLUMN DS_Observacao TEXT NULL AFTER NM_Operador'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
