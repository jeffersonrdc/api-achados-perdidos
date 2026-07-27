-- =====================================================================
-- 062_Devolucao_Notificacao_Conferencia.sql
-- Notificação ao operador + flags de conferência presencial.
-- =====================================================================

SET @schema_name = DATABASE();

SET @sql = IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'devolucao' AND COLUMN_NAME = 'FG_AtualizacaoOperador'),
  'SELECT ''FG_AtualizacaoOperador ja existe'' AS info',
  'ALTER TABLE devolucao ADD COLUMN FG_AtualizacaoOperador TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''1=solicitante atualizou; badge no painel'' AFTER TP_Status'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'devolucao' AND COLUMN_NAME = 'DT_AtualizacaoOperador'),
  'SELECT ''DT_AtualizacaoOperador ja existe'' AS info',
  'ALTER TABLE devolucao ADD COLUMN DT_AtualizacaoOperador DATETIME NULL AFTER FG_AtualizacaoOperador'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'devolucao' AND COLUMN_NAME = 'FG_ItemConferido'),
  'SELECT ''FG_ItemConferido ja existe'' AS info',
  'ALTER TABLE devolucao ADD COLUMN FG_ItemConferido TINYINT(1) NOT NULL DEFAULT 0 AFTER FG_Assinado'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'devolucao' AND COLUMN_NAME = 'FG_DocumentoConferido'),
  'SELECT ''FG_DocumentoConferido ja existe'' AS info',
  'ALTER TABLE devolucao ADD COLUMN FG_DocumentoConferido TINYINT(1) NOT NULL DEFAULT 0 AFTER FG_ItemConferido'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'devolucao' AND COLUMN_NAME = 'DT_Conferencia'),
  'SELECT ''DT_Conferencia ja existe'' AS info',
  'ALTER TABLE devolucao ADD COLUMN DT_Conferencia DATETIME NULL AFTER FG_DocumentoConferido'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- E-mail dedicado para solicitar endereço (Correios)
INSERT INTO email_parametro (TP_Evento, NM_Template, NM_Assunto)
SELECT
  'DEVOLUCAO_SHIPPING_ENDERECO',
  'devolucao-shipping-endereco.html',
  'Informe o endereço para devolução do seu item'
WHERE NOT EXISTS (
  SELECT 1 FROM email_parametro WHERE TP_Evento = 'DEVOLUCAO_SHIPPING_ENDERECO'
);

-- Copia a conta SMTP de um evento DEVOLUCAO_/CLAIM já configurado
UPDATE email_parametro p
JOIN email_parametro ref
  ON ref.TP_Evento IN ('DEVOLUCAO_ESCOLHER_MODALIDADE', 'CLAIM_APROVACAO', 'DEVOLUCAO_OCORRENCIA')
 AND ref.IDR_EmailConfig IS NOT NULL
SET p.IDR_EmailConfig = COALESCE(p.IDR_EmailConfig, ref.IDR_EmailConfig)
WHERE p.TP_Evento = 'DEVOLUCAO_SHIPPING_ENDERECO'
  AND p.IDR_EmailConfig IS NULL;

SELECT '062 ok' AS info;
