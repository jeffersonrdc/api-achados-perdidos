-- Logo/Hero do evento + suporte a persistência de imagens.
-- Idempotente.

SET @schema_name = DATABASE();

SET @sql = IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'evento' AND COLUMN_NAME = 'NM_UrlLogo'
  ),
  'SELECT ''NM_UrlLogo ja existe'' AS info',
  'ALTER TABLE evento ADD COLUMN NM_UrlLogo MEDIUMTEXT NULL AFTER QT_DiasRetencao'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'evento' AND COLUMN_NAME = 'NM_UrlHero'
  ),
  'SELECT ''NM_UrlHero ja existe'' AS info',
  'ALTER TABLE evento ADD COLUMN NM_UrlHero MEDIUMTEXT NULL AFTER NM_UrlLogo'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
