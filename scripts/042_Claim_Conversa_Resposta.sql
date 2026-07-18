-- =====================================================================
-- 042_Claim_Conversa_Resposta.sql
-- Conversa real do pedido + token temporário para /responder-email.
-- Aplicar com: mysql --default-character-set=utf8mb4
-- =====================================================================

SET @schema_name = DATABASE();

-- ---------------------------------------------------------------------
-- 1) Thread de mensagens do claim
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS claim_mensagem (
  ID_ClaimMensagem  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  IDR_Claim         BIGINT UNSIGNED NOT NULL,
  TP_Autor          VARCHAR(20)   NOT NULL COMMENT 'OPERADOR | SOLICITANTE',
  DS_Mensagem       TEXT          NOT NULL,
  IDR_Operador      BIGINT UNSIGNED NULL,
  FG_EmailEnviado   TINYINT(1)    NOT NULL DEFAULT 0,
  DS_EmailErro      VARCHAR(500)  NULL,
  DT_Mensagem       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FG_Excluido       TINYINT(1)    NOT NULL DEFAULT 0,
  CONSTRAINT FK_claimmsg_claim    FOREIGN KEY (IDR_Claim)    REFERENCES claim (ID_Claim),
  CONSTRAINT FK_claimmsg_operador FOREIGN KEY (IDR_Operador) REFERENCES usuario (ID_Usuario) ON DELETE SET NULL,
  KEY IX_claimmsg_claim_dt (IDR_Claim, DT_Mensagem)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Mensagens da conversa operador <-> solicitante';

-- ---------------------------------------------------------------------
-- 2) Token opaco de resposta (magic link)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS claim_resposta_token (
  ID_ClaimRespostaToken BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  IDR_Claim             BIGINT UNSIGNED NOT NULL,
  IDR_Mensagem          BIGINT UNSIGNED NULL,
  CD_Token              VARCHAR(64)   NOT NULL,
  DT_Expiracao          DATETIME      NOT NULL,
  DT_Usado              DATETIME      NULL,
  FG_Ativo              TINYINT(1)    NOT NULL DEFAULT 1,
  DT_Cadastro           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FG_Excluido           TINYINT(1)    NOT NULL DEFAULT 0,
  UNIQUE KEY UK_claimresptoken_cd (CD_Token),
  KEY IX_claimresptoken_claim_ativo (IDR_Claim, FG_Ativo, FG_Excluido),
  CONSTRAINT FK_claimresptoken_claim FOREIGN KEY (IDR_Claim) REFERENCES claim (ID_Claim),
  CONSTRAINT FK_claimresptoken_msg   FOREIGN KEY (IDR_Mensagem) REFERENCES claim_mensagem (ID_ClaimMensagem) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Tokens temporários do botão Enviar informações do e-mail';

-- ---------------------------------------------------------------------
-- 3) Prazo de espera aceitável (TTL do link) em evento_configuracao
-- ---------------------------------------------------------------------
SET @sql = IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'evento_configuracao'
      AND COLUMN_NAME = 'QT_DiasEsperaAceitavel'
  ),
  'SELECT ''QT_DiasEsperaAceitavel ja existe'' AS info',
  'ALTER TABLE evento_configuracao ADD COLUMN QT_DiasEsperaAceitavel INT NOT NULL DEFAULT 15 AFTER QT_DiasDescarte'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
