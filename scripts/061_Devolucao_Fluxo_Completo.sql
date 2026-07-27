-- =====================================================================
-- 061_Devolucao_Fluxo_Completo.sql
-- Fluxo PICKUP/SHIPPING: colunas em devolucao, tabelas auxiliares,
-- tokens magic-link e parametros de e-mail.
-- Aplicar com: mysql --default-character-set=utf8mb4 -u root achados_perdidos < 061_Devolucao_Fluxo_Completo.sql
-- =====================================================================

SET @schema_name = DATABASE();

-- ---------------------------------------------------------------------
-- 1) Estender devolucao
-- ---------------------------------------------------------------------
SET @sql = IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'devolucao' AND COLUMN_NAME = 'TP_Metodo'),
  'SELECT ''TP_Metodo ja existe'' AS info',
  'ALTER TABLE devolucao ADD COLUMN TP_Metodo VARCHAR(20) NULL COMMENT ''PICKUP | SHIPPING'' AFTER TP_Devolucao'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'devolucao' AND COLUMN_NAME = 'CD_Protocolo'),
  'SELECT ''CD_Protocolo ja existe'' AS info',
  'ALTER TABLE devolucao ADD COLUMN CD_Protocolo VARCHAR(40) NULL AFTER ID_Devolucao'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'devolucao' AND COLUMN_NAME = 'DT_Conclusao'),
  'SELECT ''DT_Conclusao ja existe'' AS info',
  'ALTER TABLE devolucao ADD COLUMN DT_Conclusao DATETIME NULL AFTER DT_Devolucao'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'devolucao' AND COLUMN_NAME = 'TP_ClaimOrigem'),
  'SELECT ''TP_ClaimOrigem ja existe'' AS info',
  'ALTER TABLE devolucao ADD COLUMN TP_ClaimOrigem VARCHAR(20) NULL COMMENT ''RETIRADA | PERDA'' AFTER IDR_Claim'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Amplia TP_Status para status longos do novo fluxo
ALTER TABLE devolucao MODIFY COLUMN TP_Status VARCHAR(40) NOT NULL DEFAULT 'AGUARDANDO_RETIRADA';

-- Protocolo unico (quando preenchido)
SET @sql = IF(
  EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'devolucao' AND INDEX_NAME = 'UK_devolucao_protocolo'),
  'SELECT ''UK_devolucao_protocolo ja existe'' AS info',
  'ALTER TABLE devolucao ADD UNIQUE KEY UK_devolucao_protocolo (CD_Protocolo)'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Deduplica IDR_Claim antes da UNIQUE: mantem o ticket ativo mais recente
-- (ou o maior ID se todos estiverem excluidos). Os demais sao soft-deleted
-- e perdem IDR_Claim para liberar a chave (o vinculo permanece so no ticket mantido).
UPDATE devolucao d
INNER JOIN (
  SELECT
    IDR_Claim,
    COALESCE(
      MAX(CASE WHEN COALESCE(FG_Excluido, 0) = 0 THEN ID_Devolucao END),
      MAX(ID_Devolucao)
    ) AS keep_id
  FROM devolucao
  WHERE IDR_Claim IS NOT NULL
  GROUP BY IDR_Claim
  HAVING COUNT(*) > 1
) k ON k.IDR_Claim = d.IDR_Claim AND d.ID_Devolucao <> k.keep_id
SET
  d.FG_Excluido = 1,
  d.FG_Ativo = 0,
  d.IDR_Claim = NULL;

-- Um ticket por claim (NULLs permitidos multiplos no MySQL)
SET @sql = IF(
  EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'devolucao' AND INDEX_NAME = 'UK_devolucao_claim'),
  'SELECT ''UK_devolucao_claim ja existe'' AS info',
  'ALTER TABLE devolucao ADD UNIQUE KEY UK_devolucao_claim (IDR_Claim)'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Backfill protocolo para registros existentes
UPDATE devolucao
SET CD_Protocolo = CONCAT('DEV-', YEAR(COALESCE(DT_Cadastro, NOW())), '-', LPAD(ID_Devolucao, 6, '0'))
WHERE CD_Protocolo IS NULL OR CD_Protocolo = '';

UPDATE devolucao d
JOIN claim c ON c.ID_Claim = d.IDR_Claim
SET d.TP_ClaimOrigem = c.TP_Claim
WHERE d.TP_ClaimOrigem IS NULL AND d.IDR_Claim IS NOT NULL;

-- ---------------------------------------------------------------------
-- 2) Historico append-only
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS devolucao_historico (
  ID_DevolucaoHistorico BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  IDR_Devolucao         BIGINT UNSIGNED NOT NULL,
  TP_Evento             VARCHAR(40)   NOT NULL,
  NM_Titulo             VARCHAR(200)  NOT NULL,
  DS_Descricao          TEXT          NULL,
  TP_Ator               VARCHAR(20)   NULL COMMENT 'OPERADOR | SOLICITANTE | SISTEMA',
  IDR_Operador          BIGINT UNSIGNED NULL,
  NM_Ator               VARCHAR(150)  NULL,
  FG_EmailEnviado       TINYINT(1)    NOT NULL DEFAULT 0,
  DS_EmailErro          VARCHAR(500)  NULL,
  JS_Metadata           JSON          NULL,
  DT_Evento             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FG_Excluido           TINYINT(1)    NOT NULL DEFAULT 0,
  CONSTRAINT FK_devhist_dev FOREIGN KEY (IDR_Devolucao) REFERENCES devolucao (ID_Devolucao),
  CONSTRAINT FK_devhist_op  FOREIGN KEY (IDR_Operador)  REFERENCES usuario (ID_Usuario) ON DELETE SET NULL,
  KEY IX_devhist_dev_dt (IDR_Devolucao, DT_Evento)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Timeline imutavel do ticket de devolucao';

-- ---------------------------------------------------------------------
-- 3) Token magic-link de acao
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS devolucao_acao_token (
  ID_DevolucaoAcaoToken BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  IDR_Devolucao         BIGINT UNSIGNED NOT NULL,
  TP_Acao               VARCHAR(40)   NOT NULL COMMENT 'CHOOSE_DELIVERY_METHOD | CONFIRM_PICKUP_OPTION | ...',
  CD_Token              VARCHAR(64)   NOT NULL,
  DT_Expiracao          DATETIME      NOT NULL,
  DT_Usado              DATETIME      NULL,
  FG_Ativo              TINYINT(1)    NOT NULL DEFAULT 1,
  FG_MultiUso           TINYINT(1)    NOT NULL DEFAULT 0,
  DT_Cadastro           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FG_Excluido           TINYINT(1)    NOT NULL DEFAULT 0,
  UNIQUE KEY UK_devtoken_cd (CD_Token),
  KEY IX_devtoken_dev_acao (IDR_Devolucao, TP_Acao, FG_Ativo),
  CONSTRAINT FK_devtoken_dev FOREIGN KEY (IDR_Devolucao) REFERENCES devolucao (ID_Devolucao)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Tokens de acao do solicitante (e-mail -> portal)';

-- ---------------------------------------------------------------------
-- 4) Opcoes de agenda (PICKUP)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS devolucao_pickup_opcao (
  ID_DevolucaoPickupOpcao BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  IDR_Devolucao           BIGINT UNSIGNED NOT NULL,
  DT_Opcao                DATE          NOT NULL,
  HR_Inicio               TIME          NOT NULL,
  HR_Fim                  TIME          NOT NULL,
  IDR_Local               BIGINT UNSIGNED NULL,
  NM_Local                VARCHAR(150)  NULL,
  DT_Expiracao            DATETIME      NULL,
  DS_Notas                VARCHAR(500)  NULL,
  FG_Selecionada          TINYINT(1)    NOT NULL DEFAULT 0,
  DT_Cadastro             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FG_Excluido             TINYINT(1)    NOT NULL DEFAULT 0,
  CONSTRAINT FK_devpickup_dev   FOREIGN KEY (IDR_Devolucao) REFERENCES devolucao (ID_Devolucao),
  CONSTRAINT FK_devpickup_local FOREIGN KEY (IDR_Local)     REFERENCES local (ID_Local) ON DELETE SET NULL,
  KEY IX_devpickup_dev (IDR_Devolucao, FG_Excluido)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 5) Endereco Correios
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS devolucao_shipping_endereco (
  ID_DevolucaoShippingEndereco BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  IDR_Devolucao                BIGINT UNSIGNED NOT NULL,
  NM_Destinatario              VARCHAR(150)  NOT NULL,
  NR_Cep                       VARCHAR(8)    NOT NULL,
  NM_Logradouro                VARCHAR(200)  NOT NULL,
  NR_Numero                    VARCHAR(20)   NOT NULL,
  DS_Complemento               VARCHAR(100)  NULL,
  NM_Bairro                    VARCHAR(100)  NOT NULL,
  NM_Cidade                    VARCHAR(100)  NOT NULL,
  SG_Uf                        VARCHAR(2)    NOT NULL,
  NR_Telefone                  VARCHAR(20)   NOT NULL,
  DT_Cadastro                  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  DT_Alteracao                 DATETIME      NULL,
  FG_Excluido                  TINYINT(1)    NOT NULL DEFAULT 0,
  UNIQUE KEY UK_devshipaddr_dev (IDR_Devolucao),
  CONSTRAINT FK_devshipaddr_dev FOREIGN KEY (IDR_Devolucao) REFERENCES devolucao (ID_Devolucao)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 6) Cotacao Correios
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS devolucao_shipping_cotacao (
  ID_DevolucaoShippingCotacao BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  IDR_Devolucao               BIGINT UNSIGNED NOT NULL,
  VL_Valor                    DECIMAL(12,2) NOT NULL,
  SG_Moeda                    VARCHAR(3)    NOT NULL DEFAULT 'BRL',
  QT_DiasEntregaEstimados     INT           NOT NULL DEFAULT 0,
  QT_DiasPrazoPostagem        INT           NOT NULL DEFAULT 0,
  DS_InstrucoesPagamento      TEXT          NOT NULL,
  IDR_Operador                BIGINT UNSIGNED NULL,
  DT_Informada                DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FG_Excluido                 TINYINT(1)    NOT NULL DEFAULT 0,
  KEY IX_devshipquote_dev (IDR_Devolucao, FG_Excluido),
  CONSTRAINT FK_devshipquote_dev FOREIGN KEY (IDR_Devolucao) REFERENCES devolucao (ID_Devolucao),
  CONSTRAINT FK_devshipquote_op  FOREIGN KEY (IDR_Operador)  REFERENCES usuario (ID_Usuario) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 7) Postagem / rastreio
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS devolucao_shipping_postagem (
  ID_DevolucaoShippingPostagem BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  IDR_Devolucao                BIGINT UNSIGNED NOT NULL,
  DT_Postagem                  DATE          NOT NULL,
  CD_Rastreio                  VARCHAR(40)   NOT NULL,
  DS_Notas                     VARCHAR(500)  NULL,
  IDR_Operador                 BIGINT UNSIGNED NULL,
  DT_Registro                  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FG_Excluido                  TINYINT(1)    NOT NULL DEFAULT 0,
  KEY IX_devshippost_dev (IDR_Devolucao, FG_Excluido),
  CONSTRAINT FK_devshippost_dev FOREIGN KEY (IDR_Devolucao) REFERENCES devolucao (ID_Devolucao),
  CONSTRAINT FK_devshippost_op  FOREIGN KEY (IDR_Operador)  REFERENCES usuario (ID_Usuario) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 8) Parametros de e-mail
-- ---------------------------------------------------------------------
INSERT INTO email_parametro (TP_Evento, NM_Template, NM_Assunto) VALUES
  ('DEVOLUCAO_ESCOLHER_MODALIDADE', 'devolucao-escolher-modalidade.html', 'Escolha como receber seu item — #{{protocolo}}'),
  ('DEVOLUCAO_PICKUP_OPCOES',       'devolucao-pickup-opcoes.html',       'Horarios para retirada — #{{protocolo}}'),
  ('DEVOLUCAO_PICKUP_CONFIRMADO',   'devolucao-pickup-confirmado.html',   'Agendamento confirmado — #{{protocolo}}'),
  ('DEVOLUCAO_SHIPPING_COTACAO',    'devolucao-shipping-cotacao.html',    'Cotacao de frete — #{{protocolo}}'),
  ('DEVOLUCAO_PAGAMENTO_RECEBIDO',  'devolucao-pagamento-recebido.html',  'Comprovante recebido — #{{protocolo}}'),
  ('DEVOLUCAO_POSTAGEM',            'devolucao-postagem.html',            'Seu item foi postado — #{{protocolo}}'),
  ('DEVOLUCAO_CONCLUIDA',           'devolucao-concluida.html',           'Devolucao concluida — #{{protocolo}}'),
  ('DEVOLUCAO_OCORRENCIA',          'devolucao-ocorrencia.html',          'Atualizacao da devolucao — #{{protocolo}}')
ON DUPLICATE KEY UPDATE
  NM_Template = VALUES(NM_Template),
  NM_Assunto  = VALUES(NM_Assunto);

UPDATE email_parametro p
JOIN email_parametro ref ON ref.TP_Evento = 'CLAIM_APROVACAO' AND ref.IDR_EmailConfig IS NOT NULL
SET p.IDR_EmailConfig = ref.IDR_EmailConfig
WHERE p.TP_Evento IN (
  'DEVOLUCAO_ESCOLHER_MODALIDADE', 'DEVOLUCAO_PICKUP_OPCOES', 'DEVOLUCAO_PICKUP_CONFIRMADO',
  'DEVOLUCAO_SHIPPING_COTACAO', 'DEVOLUCAO_PAGAMENTO_RECEBIDO', 'DEVOLUCAO_POSTAGEM',
  'DEVOLUCAO_CONCLUIDA', 'DEVOLUCAO_OCORRENCIA'
) AND p.IDR_EmailConfig IS NULL;
