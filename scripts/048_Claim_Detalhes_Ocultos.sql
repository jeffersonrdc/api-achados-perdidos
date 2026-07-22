-- Detalhes ocultos informados pelo solicitante na retirada (portal).

SET @schema_name = DATABASE();

SET @sql = IF(
  EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'claim'
      AND COLUMN_NAME = 'DS_DetalhesOcultos'
  ),
  'SELECT ''DS_DetalhesOcultos ja existe'' AS info',
  'ALTER TABLE claim ADD COLUMN DS_DetalhesOcultos TEXT NULL AFTER DS_Objeto'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
