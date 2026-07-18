-- =====================================================================
-- 040_Claim_Item_Perdido.sql
-- Amplia a tabela `claim` para distinguir relatos de perda (PERDA) de
-- solicitações de retirada (RETIRADA), alinhando campos ao cadastro de
-- item e ao formulário público /registrar.
--
-- Compatível com MySQL 5.7 / 8.0 (sem ADD COLUMN IF NOT EXISTS).
-- Aplicar com: mysql --default-character-set=utf8mb4
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) Colunas novas
-- ---------------------------------------------------------------------

-- TP_Claim
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'claim' AND COLUMN_NAME = 'TP_Claim'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE claim ADD COLUMN TP_Claim VARCHAR(20) NOT NULL DEFAULT ''PERDA'' AFTER IDR_Status',
  'SELECT ''coluna TP_Claim ja existe'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- CD_Claim
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'claim' AND COLUMN_NAME = 'CD_Claim'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE claim ADD COLUMN CD_Claim VARCHAR(30) NULL AFTER TP_Claim',
  'SELECT ''coluna CD_Claim ja existe'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- IDR_Subcategoria
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'claim' AND COLUMN_NAME = 'IDR_Subcategoria'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE claim ADD COLUMN IDR_Subcategoria BIGINT UNSIGNED NULL AFTER IDR_Categoria',
  'SELECT ''coluna IDR_Subcategoria ja existe'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- IDR_Local
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'claim' AND COLUMN_NAME = 'IDR_Local'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE claim ADD COLUMN IDR_Local BIGINT UNSIGNED NULL AFTER NM_Local',
  'SELECT ''coluna IDR_Local ja existe'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- NM_Estado
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'claim' AND COLUMN_NAME = 'NM_Estado'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE claim ADD COLUMN NM_Estado VARCHAR(60) NULL AFTER NM_Cor',
  'SELECT ''coluna NM_Estado ja existe'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- DS_Tags
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'claim' AND COLUMN_NAME = 'DS_Tags'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE claim ADD COLUMN DS_Tags TEXT NULL AFTER NM_Estado',
  'SELECT ''coluna DS_Tags ja existe'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- TP_Prioridade
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'claim' AND COLUMN_NAME = 'TP_Prioridade'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE claim ADD COLUMN TP_Prioridade VARCHAR(20) NULL AFTER DS_Tags',
  'SELECT ''coluna TP_Prioridade ja existe'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- FG_Sensivel
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'claim' AND COLUMN_NAME = 'FG_Sensivel'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE claim ADD COLUMN FG_Sensivel TINYINT(1) NOT NULL DEFAULT 0 AFTER TP_Prioridade',
  'SELECT ''coluna FG_Sensivel ja existe'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------
-- 2) FKs
-- ---------------------------------------------------------------------
SET @fk_exists := (
  SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'claim' AND CONSTRAINT_NAME = 'FK_claim_subcategoria'
);
SET @sql := IF(@fk_exists = 0,
  'ALTER TABLE claim ADD CONSTRAINT FK_claim_subcategoria FOREIGN KEY (IDR_Subcategoria) REFERENCES categoria (ID_Categoria)',
  'SELECT ''FK_claim_subcategoria ja existe'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists := (
  SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'claim' AND CONSTRAINT_NAME = 'FK_claim_local'
);
SET @sql := IF(@fk_exists = 0,
  'ALTER TABLE claim ADD CONSTRAINT FK_claim_local FOREIGN KEY (IDR_Local) REFERENCES local (ID_Local)',
  'SELECT ''FK_claim_local ja existe'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------
-- 3) Índices
-- ---------------------------------------------------------------------
SET @ix_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'claim' AND INDEX_NAME = 'IX_claim_evento_tipo'
);
SET @sql := IF(@ix_exists = 0,
  'ALTER TABLE claim ADD KEY IX_claim_evento_tipo (IDR_Evento, TP_Claim, FG_Excluido)',
  'SELECT ''IX_claim_evento_tipo ja existe'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ix_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'claim' AND INDEX_NAME = 'IX_claim_subcategoria'
);
SET @sql := IF(@ix_exists = 0,
  'ALTER TABLE claim ADD KEY IX_claim_subcategoria (IDR_Subcategoria)',
  'SELECT ''IX_claim_subcategoria ja existe'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ix_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'claim' AND INDEX_NAME = 'IX_claim_local'
);
SET @sql := IF(@ix_exists = 0,
  'ALTER TABLE claim ADD KEY IX_claim_local (IDR_Local)',
  'SELECT ''IX_claim_local ja existe'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ix_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'claim' AND INDEX_NAME = 'UK_claim_cd'
);
SET @sql := IF(@ix_exists = 0,
  'ALTER TABLE claim ADD UNIQUE KEY UK_claim_cd (CD_Claim)',
  'SELECT ''UK_claim_cd ja existe'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------
-- 4) Backfill
-- ---------------------------------------------------------------------
-- Claims com validação vinculada a item => RETIRADA; demais => PERDA
UPDATE claim c
SET c.TP_Claim = 'RETIRADA'
WHERE EXISTS (
  SELECT 1 FROM claim_validacao cv
  WHERE cv.IDR_Claim = c.ID_Claim
    AND cv.FG_Excluido = 0
    AND cv.IDR_Item IS NOT NULL
)
AND (c.TP_Claim IS NULL OR c.TP_Claim = '' OR c.TP_Claim = 'PERDA');

UPDATE claim
SET TP_Claim = 'PERDA'
WHERE TP_Claim IS NULL OR TP_Claim = '';

-- Protocolo para registros sem CD_Claim
UPDATE claim
SET CD_Claim = CONCAT('CLM-', YEAR(IFNULL(DT_Cadastro, NOW())), '-', LPAD(ID_Claim, 5, '0'))
WHERE CD_Claim IS NULL OR CD_Claim = '';
