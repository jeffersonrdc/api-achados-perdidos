-- =====================================================================
-- 059_Portal_Contatos_Wallpapers.sql
-- Contatos do portal (sistema_parametro) + limite de wallpapers por evento.
-- Aplicar com: mysql --default-character-set=utf8mb4 -u root achados_perdidos < 059_Portal_Contatos_Wallpapers.sql
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) Canais de contato do portal publico (globais)
-- ---------------------------------------------------------------------
INSERT INTO sistema_parametro (NM_Chave, DS_Valor, DS_Descricao) VALUES
  ('PORTAL_TELEFONE_CENTRAL', '(21) 3900-0010', 'Telefone da central de atendimento exibido no portal /contato'),
  ('PORTAL_WHATSAPP', '(21) 99801-0000', 'WhatsApp oficial exibido no portal /contato'),
  ('PORTAL_EMAIL_SUPORTE', 'achados@rockinrio.com', 'E-mail de suporte exibido no portal /contato')
ON DUPLICATE KEY UPDATE
  DS_Descricao = VALUES(DS_Descricao);

-- ---------------------------------------------------------------------
-- 2) Quantidade maxima de wallpapers por evento
-- ---------------------------------------------------------------------
SET @col_existe := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'evento_configuracao'
    AND COLUMN_NAME = 'QT_WallpapersDisponiveis'
);

SET @sql := IF(
  @col_existe = 0,
  'ALTER TABLE evento_configuracao ADD COLUMN QT_WallpapersDisponiveis INT NOT NULL DEFAULT 6 COMMENT ''Limite de wallpapers no portal'' AFTER QT_DiasEsperaAceitavel',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
