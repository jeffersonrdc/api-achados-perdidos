-- Tags de características do item coletado (DS_Tags).
-- Necessário para aceitar dsTags no POST/PUT /api/v1/itens.

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'item' AND COLUMN_NAME = 'DS_Tags'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE item ADD COLUMN DS_Tags TEXT NULL AFTER NM_Estado',
  'SELECT ''coluna DS_Tags ja existe'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
