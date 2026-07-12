-- =====================================================================
-- 021_Auditoria_FKs_Novas_Tabelas.sql
-- Corrige lacunas das tabelas criadas em 019/020 para ficarem no mesmo
-- padrao do projeto:
--   1) FKs em IDR_UsuarioCadastro / IDR_UsuarioAlteracao -> usuario
--   2) Trigger AFTER INSERT  -> auditoria (SP_RegistrarAuditoria)
--   3) Trigger AFTER UPDATE  -> auditoria quando FG_Excluido muda
--   4) Trigger BEFORE DELETE -> bloqueia exclusao fisica (soft delete)
-- Tabelas: local, equipe, equipe_usuario, triagem, etiqueta_impressao
-- Executar uma unica vez.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) FKs de auditoria de usuario
-- ---------------------------------------------------------------------
ALTER TABLE local
  ADD CONSTRAINT FK_local_usuario_cad FOREIGN KEY (IDR_UsuarioCadastro)  REFERENCES usuario (ID_Usuario),
  ADD CONSTRAINT FK_local_usuario_alt FOREIGN KEY (IDR_UsuarioAlteracao) REFERENCES usuario (ID_Usuario);

ALTER TABLE equipe
  ADD CONSTRAINT FK_equipe_usuario_cad FOREIGN KEY (IDR_UsuarioCadastro)  REFERENCES usuario (ID_Usuario),
  ADD CONSTRAINT FK_equipe_usuario_alt FOREIGN KEY (IDR_UsuarioAlteracao) REFERENCES usuario (ID_Usuario);

ALTER TABLE equipe_usuario
  ADD CONSTRAINT FK_equipeusuario_usuario_cad FOREIGN KEY (IDR_UsuarioCadastro)  REFERENCES usuario (ID_Usuario),
  ADD CONSTRAINT FK_equipeusuario_usuario_alt FOREIGN KEY (IDR_UsuarioAlteracao) REFERENCES usuario (ID_Usuario);

ALTER TABLE triagem
  ADD CONSTRAINT FK_triagem_usuario_cad FOREIGN KEY (IDR_UsuarioCadastro)  REFERENCES usuario (ID_Usuario),
  ADD CONSTRAINT FK_triagem_usuario_alt FOREIGN KEY (IDR_UsuarioAlteracao) REFERENCES usuario (ID_Usuario);

ALTER TABLE etiqueta_impressao
  ADD CONSTRAINT FK_etiqueta_usuario_cad FOREIGN KEY (IDR_UsuarioCadastro)  REFERENCES usuario (ID_Usuario),
  ADD CONSTRAINT FK_etiqueta_usuario_alt FOREIGN KEY (IDR_UsuarioAlteracao) REFERENCES usuario (ID_Usuario);

-- ---------------------------------------------------------------------
-- 2/3/4) Triggers de auditoria + bloqueio de exclusao fisica
-- ---------------------------------------------------------------------
DROP TRIGGER IF EXISTS TRG_local_ai_audit;
DROP TRIGGER IF EXISTS TRG_local_au_audit;
DROP TRIGGER IF EXISTS TRG_local_bd_softdelete;
DROP TRIGGER IF EXISTS TRG_equipe_ai_audit;
DROP TRIGGER IF EXISTS TRG_equipe_au_audit;
DROP TRIGGER IF EXISTS TRG_equipe_bd_softdelete;
DROP TRIGGER IF EXISTS TRG_equipeusuario_ai_audit;
DROP TRIGGER IF EXISTS TRG_equipeusuario_au_audit;
DROP TRIGGER IF EXISTS TRG_equipeusuario_bd_softdelete;
DROP TRIGGER IF EXISTS TRG_triagem_ai_audit;
DROP TRIGGER IF EXISTS TRG_triagem_au_audit;
DROP TRIGGER IF EXISTS TRG_triagem_bd_softdelete;
DROP TRIGGER IF EXISTS TRG_etiqueta_ai_audit;
DROP TRIGGER IF EXISTS TRG_etiqueta_au_audit;
DROP TRIGGER IF EXISTS TRG_etiqueta_bd_softdelete;

DELIMITER $$

-- local -------------------------------------------------------------
CREATE TRIGGER TRG_local_ai_audit AFTER INSERT ON local FOR EACH ROW
BEGIN
  CALL SP_RegistrarAuditoria('local', NEW.ID_Local, 'INSERT', NULL,
    JSON_OBJECT('NM_Local', NEW.NM_Local, 'TP_Local', NEW.TP_Local), NEW.IDR_UsuarioCadastro);
END$$
CREATE TRIGGER TRG_local_au_audit AFTER UPDATE ON local FOR EACH ROW
BEGIN
  IF OLD.FG_Excluido <> NEW.FG_Excluido THEN
    CALL SP_RegistrarAuditoria('local', NEW.ID_Local, 'UPDATE',
      JSON_OBJECT('FG_Excluido', OLD.FG_Excluido), JSON_OBJECT('FG_Excluido', NEW.FG_Excluido), NEW.IDR_UsuarioAlteracao);
  END IF;
END$$
CREATE TRIGGER TRG_local_bd_softdelete BEFORE DELETE ON local FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Exclusão física não permitida. Use soft delete (FG_Excluido = 1).';
END$$

-- equipe ------------------------------------------------------------
CREATE TRIGGER TRG_equipe_ai_audit AFTER INSERT ON equipe FOR EACH ROW
BEGIN
  CALL SP_RegistrarAuditoria('equipe', NEW.ID_Equipe, 'INSERT', NULL,
    JSON_OBJECT('NM_Equipe', NEW.NM_Equipe, 'TP_Equipe', NEW.TP_Equipe), NEW.IDR_UsuarioCadastro);
END$$
CREATE TRIGGER TRG_equipe_au_audit AFTER UPDATE ON equipe FOR EACH ROW
BEGIN
  IF OLD.FG_Excluido <> NEW.FG_Excluido THEN
    CALL SP_RegistrarAuditoria('equipe', NEW.ID_Equipe, 'UPDATE',
      JSON_OBJECT('FG_Excluido', OLD.FG_Excluido), JSON_OBJECT('FG_Excluido', NEW.FG_Excluido), NEW.IDR_UsuarioAlteracao);
  END IF;
END$$
CREATE TRIGGER TRG_equipe_bd_softdelete BEFORE DELETE ON equipe FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Exclusão física não permitida. Use soft delete (FG_Excluido = 1).';
END$$

-- equipe_usuario ----------------------------------------------------
CREATE TRIGGER TRG_equipeusuario_ai_audit AFTER INSERT ON equipe_usuario FOR EACH ROW
BEGIN
  CALL SP_RegistrarAuditoria('equipe_usuario', NEW.ID_EquipeUsuario, 'INSERT', NULL,
    JSON_OBJECT('IDR_Equipe', NEW.IDR_Equipe, 'IDR_Usuario', NEW.IDR_Usuario), NEW.IDR_UsuarioCadastro);
END$$
CREATE TRIGGER TRG_equipeusuario_au_audit AFTER UPDATE ON equipe_usuario FOR EACH ROW
BEGIN
  IF OLD.FG_Excluido <> NEW.FG_Excluido THEN
    CALL SP_RegistrarAuditoria('equipe_usuario', NEW.ID_EquipeUsuario, 'UPDATE',
      JSON_OBJECT('FG_Excluido', OLD.FG_Excluido), JSON_OBJECT('FG_Excluido', NEW.FG_Excluido), NEW.IDR_UsuarioAlteracao);
  END IF;
END$$
CREATE TRIGGER TRG_equipeusuario_bd_softdelete BEFORE DELETE ON equipe_usuario FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Exclusão física não permitida. Use soft delete (FG_Excluido = 1).';
END$$

-- triagem -----------------------------------------------------------
CREATE TRIGGER TRG_triagem_ai_audit AFTER INSERT ON triagem FOR EACH ROW
BEGIN
  CALL SP_RegistrarAuditoria('triagem', NEW.ID_Triagem, 'INSERT', NULL,
    JSON_OBJECT('IDR_Item', NEW.IDR_Item, 'TP_Status', NEW.TP_Status), NEW.IDR_UsuarioCadastro);
END$$
CREATE TRIGGER TRG_triagem_au_audit AFTER UPDATE ON triagem FOR EACH ROW
BEGIN
  IF OLD.TP_Status <> NEW.TP_Status OR OLD.FG_Excluido <> NEW.FG_Excluido THEN
    CALL SP_RegistrarAuditoria('triagem', NEW.ID_Triagem, 'UPDATE',
      JSON_OBJECT('TP_Status', OLD.TP_Status, 'FG_Excluido', OLD.FG_Excluido),
      JSON_OBJECT('TP_Status', NEW.TP_Status, 'FG_Excluido', NEW.FG_Excluido), NEW.IDR_UsuarioAlteracao);
  END IF;
END$$
CREATE TRIGGER TRG_triagem_bd_softdelete BEFORE DELETE ON triagem FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Exclusão física não permitida. Use soft delete (FG_Excluido = 1).';
END$$

-- etiqueta_impressao ------------------------------------------------
CREATE TRIGGER TRG_etiqueta_ai_audit AFTER INSERT ON etiqueta_impressao FOR EACH ROW
BEGIN
  CALL SP_RegistrarAuditoria('etiqueta_impressao', NEW.ID_EtiquetaImpressao, 'INSERT', NULL,
    JSON_OBJECT('IDR_Item', NEW.IDR_Item, 'TP_Impressao', NEW.TP_Impressao, 'NM_Impressora', NEW.NM_Impressora), NEW.IDR_UsuarioCadastro);
END$$
CREATE TRIGGER TRG_etiqueta_au_audit AFTER UPDATE ON etiqueta_impressao FOR EACH ROW
BEGIN
  IF OLD.FG_Excluido <> NEW.FG_Excluido THEN
    CALL SP_RegistrarAuditoria('etiqueta_impressao', NEW.ID_EtiquetaImpressao, 'UPDATE',
      JSON_OBJECT('FG_Excluido', OLD.FG_Excluido), JSON_OBJECT('FG_Excluido', NEW.FG_Excluido), NEW.IDR_UsuarioAlteracao);
  END IF;
END$$
CREATE TRIGGER TRG_etiqueta_bd_softdelete BEFORE DELETE ON etiqueta_impressao FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Exclusão física não permitida. Use soft delete (FG_Excluido = 1).';
END$$

DELIMITER ;
