-- =====================================================================
-- 036_Email_Claim_Workflow.sql
-- Fluxo de pedido de devolução (claim): e-mails configuráveis + histórico.
--
--   1) email_config    -> contas SMTP (VÁRIAS) configuráveis em /configuracoes.
--   2) email_parametro -> qual conta envia em cada situação (análise, info, ...).
--   3) claim_historico -> ciclo de vida do claim (análise/info/aprovação/reprovação),
--                         base do histórico de reprovações por item.
--   4) status "Claim Aguardando Info".
-- Aplicar com: mysql --default-character-set=utf8mb4
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) Contas SMTP (múltiplas)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS email_config (
  ID_EmailConfig    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  NM_Config         VARCHAR(120)  NOT NULL COMMENT 'Rótulo da conta (ex.: "SMTP Principal")',
  NM_Host           VARCHAR(150)  NULL,
  NR_Porta          INT           NULL,
  NM_Usuario        VARCHAR(200)  NULL,
  NM_Senha          VARCHAR(255)  NULL,
  NM_Remetente      VARCHAR(200)  NULL COMMENT 'E-mail remetente (From)',
  NM_RemetenteNome  VARCHAR(150)  NULL,
  FG_Tls            TINYINT(1)    NOT NULL DEFAULT 1,
  DT_Cadastro       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  DT_Alteracao      DATETIME      NULL ON UPDATE CURRENT_TIMESTAMP,
  FG_Ativo          TINYINT(1)    NOT NULL DEFAULT 1,
  FG_Excluido       TINYINT(1)    NOT NULL DEFAULT 0,
  KEY IX_emailconfig_ativo (FG_Ativo, FG_Excluido)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Contas SMTP para envio de e-mail';

-- ---------------------------------------------------------------------
-- 2) Parâmetro: propósito -> conta + template
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS email_parametro (
  ID_EmailParametro BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  TP_Evento         VARCHAR(40)   NOT NULL COMMENT 'CLAIM_ANALISE | CLAIM_SOLICITACAO_INFO | CLAIM_APROVACAO | CLAIM_REPROVACAO',
  IDR_EmailConfig   BIGINT UNSIGNED NULL,
  NM_Template       VARCHAR(120)  NOT NULL COMMENT 'Arquivo em resources/templates/email',
  NM_Assunto        VARCHAR(200)  NULL,
  DT_Cadastro       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  DT_Alteracao      DATETIME      NULL ON UPDATE CURRENT_TIMESTAMP,
  FG_Ativo          TINYINT(1)    NOT NULL DEFAULT 1,
  UNIQUE KEY UK_emailparam_evento (TP_Evento),
  CONSTRAINT FK_emailparam_config FOREIGN KEY (IDR_EmailConfig) REFERENCES email_config (ID_EmailConfig) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Mapeia cada e-mail do fluxo à conta/template';

INSERT INTO email_parametro (TP_Evento, NM_Template, NM_Assunto) VALUES
  ('CLAIM_ANALISE',          'claim-analise.html',         'Seu pedido de devolução está em análise'),
  ('CLAIM_SOLICITACAO_INFO', 'claim-solicitacao-info.html','Precisamos de mais informações sobre seu pedido'),
  ('CLAIM_APROVACAO',        'claim-aprovacao.html',       'Seu pedido de devolução foi aprovado'),
  ('CLAIM_REPROVACAO',       'claim-reprovacao.html',      'Atualização sobre seu pedido de devolução')
ON DUPLICATE KEY UPDATE NM_Template = VALUES(NM_Template);

-- ---------------------------------------------------------------------
-- 3) Histórico do claim (ciclo de vida + comunicação + reprovações)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS claim_historico (
  ID_ClaimHistorico BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  IDR_Claim         BIGINT UNSIGNED NOT NULL,
  IDR_Item          BIGINT UNSIGNED NULL,
  TP_Evento         VARCHAR(30)   NOT NULL COMMENT 'ANALISE | SOLICITACAO_INFO | APROVACAO | REPROVACAO',
  TP_Solicitacao    VARCHAR(20)   NULL COMMENT 'PERGUNTA | IMAGEM (quando SOLICITACAO_INFO)',
  DS_Detalhe        TEXT          NULL COMMENT 'Pergunta / motivo / observação',
  IDR_Operador      BIGINT UNSIGNED NULL,
  FG_EmailEnviado   TINYINT(1)    NOT NULL DEFAULT 0,
  DS_EmailErro      VARCHAR(500)  NULL,
  DT_Historico      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FG_Excluido       TINYINT(1)    NOT NULL DEFAULT 0,
  CONSTRAINT FK_claimhist_claim    FOREIGN KEY (IDR_Claim)    REFERENCES claim (ID_Claim),
  CONSTRAINT FK_claimhist_item     FOREIGN KEY (IDR_Item)     REFERENCES item (ID_Item),
  CONSTRAINT FK_claimhist_operador FOREIGN KEY (IDR_Operador) REFERENCES usuario (ID_Usuario) ON DELETE SET NULL,
  KEY IX_claimhist_claim (IDR_Claim),
  KEY IX_claimhist_item_evento (IDR_Item, TP_Evento)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Histórico/comunicação dos pedidos de devolução';

-- ---------------------------------------------------------------------
-- 4) Novo status para o passo de solicitação de informações
-- ---------------------------------------------------------------------
INSERT INTO status_item (NM_Status, DS_Status, OR_Ordem, FG_Final)
SELECT 'Claim Aguardando Info', 'Aguardando informações adicionais do solicitante', 95, 0
WHERE NOT EXISTS (SELECT 1 FROM status_item WHERE NM_Status = 'Claim Aguardando Info');

-- ---------------------------------------------------------------------
-- 5) Corrige o acento corrompido (mojibake) do status "Claim em Análise",
--    que impedia o lookup por nome (e o STATUS_OCULTOS_COLETA do backend).
-- ---------------------------------------------------------------------
UPDATE status_item SET NM_Status = 'Claim em Análise'
WHERE NM_Status LIKE 'Claim em An%lise' AND NM_Status <> 'Claim em Análise';
