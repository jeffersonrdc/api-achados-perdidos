-- Contato de confiança no claim (portal: perda e retirada).

SET @schema_name = DATABASE();

SET @sql = IF(
  EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'claim'
      AND COLUMN_NAME = 'NM_ContatoConfianca'
  ),
  'SELECT ''NM_ContatoConfianca ja existe'' AS info',
  'ALTER TABLE claim ADD COLUMN NM_ContatoConfianca VARCHAR(150) NULL AFTER NR_Telefone'
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
      AND COLUMN_NAME = 'NR_TelefoneConfianca'
  ),
  'SELECT ''NR_TelefoneConfianca ja existe'' AS info',
  'ALTER TABLE claim ADD COLUMN NR_TelefoneConfianca VARCHAR(20) NULL AFTER NM_ContatoConfianca'
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
      AND COLUMN_NAME = 'DS_RelacaoContatoConfianca'
  ),
  'SELECT ''DS_RelacaoContatoConfianca ja existe'' AS info',
  'ALTER TABLE claim ADD COLUMN DS_RelacaoContatoConfianca VARCHAR(80) NULL AFTER NR_TelefoneConfianca'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
