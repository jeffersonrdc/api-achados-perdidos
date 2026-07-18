-- =====================================================================
-- 039_Estoque_Endereco_Hierarquia.sql
-- Hierarquia opcional no catálogo de endereçamento (setor → estante →
-- prateleira → caixa → posição) para a tela /logistica-fisica.
--
-- Compatível com MySQL 5.7 / 8.0 (sem ADD COLUMN IF NOT EXISTS).
-- Aplicar com: mysql --default-character-set=utf8mb4
-- =====================================================================

-- Coluna IDR_EnderecoPai (idempotente)
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'estoque_endereco'
    AND COLUMN_NAME = 'IDR_EnderecoPai'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE estoque_endereco ADD COLUMN IDR_EnderecoPai BIGINT UNSIGNED NULL AFTER IDR_Deposito',
  'SELECT ''coluna IDR_EnderecoPai ja existe'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- FK (idempotente)
SET @fk_exists := (
  SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'estoque_endereco'
    AND CONSTRAINT_NAME = 'FK_endereco_pai'
);
SET @sql := IF(@fk_exists = 0,
  'ALTER TABLE estoque_endereco ADD CONSTRAINT FK_endereco_pai FOREIGN KEY (IDR_EnderecoPai) REFERENCES estoque_endereco (ID_Endereco)',
  'SELECT ''FK_endereco_pai ja existe'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Índice (idempotente)
SET @ix_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'estoque_endereco'
    AND INDEX_NAME = 'IX_endereco_pai'
);
SET @sql := IF(@ix_exists = 0,
  'ALTER TABLE estoque_endereco ADD KEY IX_endereco_pai (IDR_EnderecoPai)',
  'SELECT ''IX_endereco_pai ja existe'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
