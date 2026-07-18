-- =====================================================================
-- 046_Arquivo_Storage_Provider.sql
-- Armazenamento híbrido Local / AWS S3:
--   1) TP_Storage por arquivo (coexistência após troca do provedor padrão)
--   2) sistema_parametro com ARQUIVO_STORAGE_PROVIDER (LOCAL|S3)
-- Aplicar com: mysql --default-character-set=utf8mb4
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) Provedor físico por registro de arquivo
-- ---------------------------------------------------------------------
SET @col_existe := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'arquivo'
    AND COLUMN_NAME = 'TP_Storage'
);

SET @sql := IF(
  @col_existe = 0,
  'ALTER TABLE arquivo ADD COLUMN TP_Storage VARCHAR(10) NOT NULL DEFAULT ''LOCAL'' COMMENT ''LOCAL|S3 — provedor físico deste arquivo'' AFTER NM_Path',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE arquivo SET TP_Storage = 'LOCAL' WHERE TP_Storage IS NULL OR TP_Storage = '';

-- ---------------------------------------------------------------------
-- 2) Parâmetros globais do sistema (chave/valor)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sistema_parametro (
  NM_Chave      VARCHAR(80)  NOT NULL COMMENT 'Chave única do parâmetro',
  DS_Valor      VARCHAR(500) NULL,
  DS_Descricao  VARCHAR(255) NULL,
  DT_Cadastro   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  DT_Alteracao  DATETIME     NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (NM_Chave)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Parâmetros globais do sistema (ex.: provedor de armazenamento)';

INSERT INTO sistema_parametro (NM_Chave, DS_Valor, DS_Descricao)
VALUES (
  'ARQUIVO_STORAGE_PROVIDER',
  'LOCAL',
  'Provedor padrão para novos uploads: LOCAL ou S3'
)
ON DUPLICATE KEY UPDATE DS_Descricao = VALUES(DS_Descricao);
