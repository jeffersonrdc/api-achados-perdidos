-- =====================================================================
-- 032_Transferencia.sql
-- Transferência = alguém pega itens de um LOCAL (origem) e leva para outro
-- LOCAL (destino). Persistida e auditada. Difere do "estoque" (endereçamento
-- do item DENTRO de um depósito: setor/estante/caixa via `localizacao`).
--
--   1) item.IDR_LocalAtual = local físico atual do item (muda a cada transferência).
--   2) tabela `transferencia` (1 linha por item transferido) + FKs.
--   3) triggers de auditoria (padrão SP_RegistrarAuditoria) + trava anti-delete.
--   4) backfill do IDR_LocalAtual (por NM_LocalEncontrado; fallback = depósito).
-- Aplicar com: mysql --default-character-set=utf8mb4
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) Local atual do item
-- ---------------------------------------------------------------------
ALTER TABLE item ADD COLUMN IDR_LocalAtual BIGINT UNSIGNED NULL AFTER IDR_Localizacao;
ALTER TABLE item ADD CONSTRAINT FK_item_local_atual FOREIGN KEY (IDR_LocalAtual) REFERENCES local (ID_Local);
ALTER TABLE item ADD INDEX IX_item_local_atual (IDR_LocalAtual);

-- Backfill: casa o local encontrado com um local do evento; senão, cai no depósito.
UPDATE item i
  JOIN local l ON l.IDR_Evento = i.IDR_Evento
              AND l.NM_Local = i.NM_LocalEncontrado COLLATE utf8mb4_unicode_ci
              AND l.FG_Excluido = 0
   SET i.IDR_LocalAtual = l.ID_Local
 WHERE i.FG_Excluido = 0 AND i.IDR_LocalAtual IS NULL;

UPDATE item i
  JOIN local l ON l.IDR_Evento = i.IDR_Evento AND l.TP_Local = 'DEPOSITO' AND l.FG_Excluido = 0
   SET i.IDR_LocalAtual = l.ID_Local
 WHERE i.FG_Excluido = 0 AND i.IDR_LocalAtual IS NULL;

-- ---------------------------------------------------------------------
-- 2) Tabela transferencia
-- ---------------------------------------------------------------------
CREATE TABLE transferencia (
  ID_Transferencia        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  IDR_Evento              BIGINT UNSIGNED NOT NULL,
  IDR_Item                BIGINT UNSIGNED NOT NULL,
  IDR_LocalOrigem         BIGINT UNSIGNED NULL,
  IDR_LocalDestino        BIGINT UNSIGNED NOT NULL,
  IDR_UsuarioResponsavel  BIGINT UNSIGNED NULL,
  NM_Receptor             VARCHAR(150) NULL,
  DS_Motivo               VARCHAR(500) NULL,
  TP_Status               VARCHAR(30)  NOT NULL DEFAULT 'CONCLUIDA',
  DT_Transferencia        DATETIME     NOT NULL,
  DT_Cadastro             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  DT_Alteracao            DATETIME     NULL ON UPDATE CURRENT_TIMESTAMP,
  IDR_UsuarioCadastro     BIGINT UNSIGNED NULL,
  IDR_UsuarioAlteracao    BIGINT UNSIGNED NULL,
  FG_Ativo                TINYINT(1)   NOT NULL DEFAULT 1,
  FG_Excluido             TINYINT(1)   NOT NULL DEFAULT 0,
  CONSTRAINT FK_transf_evento       FOREIGN KEY (IDR_Evento)             REFERENCES evento (ID_Evento),
  CONSTRAINT FK_transf_item         FOREIGN KEY (IDR_Item)               REFERENCES item (ID_Item),
  CONSTRAINT FK_transf_local_orig   FOREIGN KEY (IDR_LocalOrigem)        REFERENCES local (ID_Local),
  CONSTRAINT FK_transf_local_dest   FOREIGN KEY (IDR_LocalDestino)       REFERENCES local (ID_Local),
  CONSTRAINT FK_transf_usuario_resp FOREIGN KEY (IDR_UsuarioResponsavel) REFERENCES usuario (ID_Usuario),
  CONSTRAINT FK_transf_usuario_cad  FOREIGN KEY (IDR_UsuarioCadastro)    REFERENCES usuario (ID_Usuario),
  CONSTRAINT FK_transf_usuario_alt  FOREIGN KEY (IDR_UsuarioAlteracao)   REFERENCES usuario (ID_Usuario),
  INDEX IX_transf_evento (IDR_Evento),
  INDEX IX_transf_item (IDR_Item),
  INDEX IX_transf_data (DT_Transferencia)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 3) Auditoria (mesmo padrão das demais tabelas)
-- ---------------------------------------------------------------------
DROP TRIGGER IF EXISTS TRG_transferencia_ai_audit;
DROP TRIGGER IF EXISTS TRG_transferencia_au_audit;
DROP TRIGGER IF EXISTS TRG_transferencia_bd_softdelete;
DELIMITER $$
CREATE TRIGGER TRG_transferencia_ai_audit AFTER INSERT ON transferencia FOR EACH ROW
BEGIN
  CALL SP_RegistrarAuditoria('transferencia', NEW.ID_Transferencia, 'INSERT', NULL,
    JSON_OBJECT('IDR_Item', NEW.IDR_Item, 'IDR_LocalOrigem', NEW.IDR_LocalOrigem,
                'IDR_LocalDestino', NEW.IDR_LocalDestino, 'DS_Motivo', NEW.DS_Motivo,
                'TP_Status', NEW.TP_Status), NEW.IDR_UsuarioCadastro);
END$$
CREATE TRIGGER TRG_transferencia_au_audit AFTER UPDATE ON transferencia FOR EACH ROW
BEGIN
  CALL SP_RegistrarAuditoria('transferencia', NEW.ID_Transferencia, 'UPDATE',
    JSON_OBJECT('TP_Status', OLD.TP_Status, 'FG_Excluido', OLD.FG_Excluido),
    JSON_OBJECT('TP_Status', NEW.TP_Status, 'FG_Excluido', NEW.FG_Excluido), NEW.IDR_UsuarioAlteracao);
END$$
CREATE TRIGGER TRG_transferencia_bd_softdelete BEFORE DELETE ON transferencia FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Exclusão física não permitida. Use soft delete (FG_Excluido = 1).';
END$$
DELIMITER ;
