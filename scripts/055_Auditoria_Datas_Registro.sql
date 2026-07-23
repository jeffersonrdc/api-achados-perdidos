-- =====================================================================
-- 055_Auditoria_Datas_Registro.sql
-- Inclui DT_Cadastro / DT_Alteracao nos snapshots JSON da auditoria
-- (categoria, local, usuario, equipe, triagem) para a tela /logs
-- exibir quando o registro foi criado ou atualizado.
-- Executar uma única vez.
-- =====================================================================

DROP TRIGGER IF EXISTS TRG_categoria_ai_audit;
DROP TRIGGER IF EXISTS TRG_categoria_au_audit;
DROP TRIGGER IF EXISTS TRG_local_ai_audit;
DROP TRIGGER IF EXISTS TRG_local_au_audit;
DROP TRIGGER IF EXISTS TRG_usuario_ai_audit;
DROP TRIGGER IF EXISTS TRG_usuario_au_audit;
DROP TRIGGER IF EXISTS TRG_equipe_ai_audit;
DROP TRIGGER IF EXISTS TRG_equipe_au_audit;
DROP TRIGGER IF EXISTS TRG_triagem_ai_audit;
DROP TRIGGER IF EXISTS TRG_triagem_au_audit;

DELIMITER $$

-- categoria ---------------------------------------------------------
CREATE TRIGGER TRG_categoria_ai_audit AFTER INSERT ON categoria FOR EACH ROW
BEGIN
  CALL SP_RegistrarAuditoria('categoria', NEW.ID_Categoria, 'INSERT', NULL,
    JSON_OBJECT('NM_Categoria', NEW.NM_Categoria, 'DS_Categoria', NEW.DS_Categoria,
                'IC_Icone', NEW.IC_Icone, 'OR_Ordem', NEW.OR_Ordem,
                'FG_Ativo', NEW.FG_Ativo, 'FG_Excluido', NEW.FG_Excluido,
                'DT_Cadastro', DATE_FORMAT(NEW.DT_Cadastro, '%Y-%m-%d %H:%i:%s'),
                'DT_Alteracao', DATE_FORMAT(NEW.DT_Alteracao, '%Y-%m-%d %H:%i:%s')),
    NEW.IDR_UsuarioCadastro);
END$$
CREATE TRIGGER TRG_categoria_au_audit AFTER UPDATE ON categoria FOR EACH ROW
BEGIN
  IF NOT (OLD.NM_Categoria <=> NEW.NM_Categoria AND OLD.DS_Categoria <=> NEW.DS_Categoria
          AND OLD.IC_Icone <=> NEW.IC_Icone AND OLD.OR_Ordem <=> NEW.OR_Ordem
          AND OLD.FG_Ativo <=> NEW.FG_Ativo AND OLD.FG_Excluido <=> NEW.FG_Excluido) THEN
    CALL SP_RegistrarAuditoria('categoria', NEW.ID_Categoria, 'UPDATE',
      JSON_OBJECT('NM_Categoria', OLD.NM_Categoria, 'DS_Categoria', OLD.DS_Categoria,
                  'IC_Icone', OLD.IC_Icone, 'OR_Ordem', OLD.OR_Ordem,
                  'FG_Ativo', OLD.FG_Ativo, 'FG_Excluido', OLD.FG_Excluido,
                  'DT_Cadastro', DATE_FORMAT(OLD.DT_Cadastro, '%Y-%m-%d %H:%i:%s'),
                  'DT_Alteracao', DATE_FORMAT(OLD.DT_Alteracao, '%Y-%m-%d %H:%i:%s')),
      JSON_OBJECT('NM_Categoria', NEW.NM_Categoria, 'DS_Categoria', NEW.DS_Categoria,
                  'IC_Icone', NEW.IC_Icone, 'OR_Ordem', NEW.OR_Ordem,
                  'FG_Ativo', NEW.FG_Ativo, 'FG_Excluido', NEW.FG_Excluido,
                  'DT_Cadastro', DATE_FORMAT(NEW.DT_Cadastro, '%Y-%m-%d %H:%i:%s'),
                  'DT_Alteracao', DATE_FORMAT(NEW.DT_Alteracao, '%Y-%m-%d %H:%i:%s')),
      NEW.IDR_UsuarioAlteracao);
  END IF;
END$$

-- local -------------------------------------------------------------
CREATE TRIGGER TRG_local_ai_audit AFTER INSERT ON local FOR EACH ROW
BEGIN
  CALL SP_RegistrarAuditoria('local', NEW.ID_Local, 'INSERT', NULL,
    JSON_OBJECT('NM_Local', NEW.NM_Local, 'TP_Local', NEW.TP_Local,
                'IDR_Responsavel', NEW.IDR_Responsavel, 'VL_Latitude', NEW.VL_Latitude,
                'VL_Longitude', NEW.VL_Longitude, 'NM_Horario', NEW.NM_Horario,
                'DS_Observacao', NEW.DS_Observacao, 'FG_Ativo', NEW.FG_Ativo,
                'FG_Excluido', NEW.FG_Excluido,
                'DT_Cadastro', DATE_FORMAT(NEW.DT_Cadastro, '%Y-%m-%d %H:%i:%s'),
                'DT_Alteracao', DATE_FORMAT(NEW.DT_Alteracao, '%Y-%m-%d %H:%i:%s')),
    NEW.IDR_UsuarioCadastro);
END$$
CREATE TRIGGER TRG_local_au_audit AFTER UPDATE ON local FOR EACH ROW
BEGIN
  IF NOT (OLD.NM_Local <=> NEW.NM_Local AND OLD.TP_Local <=> NEW.TP_Local
          AND OLD.IDR_Responsavel <=> NEW.IDR_Responsavel AND OLD.VL_Latitude <=> NEW.VL_Latitude
          AND OLD.VL_Longitude <=> NEW.VL_Longitude AND OLD.NM_Horario <=> NEW.NM_Horario
          AND OLD.DS_Observacao <=> NEW.DS_Observacao AND OLD.FG_Ativo <=> NEW.FG_Ativo
          AND OLD.FG_Excluido <=> NEW.FG_Excluido) THEN
    CALL SP_RegistrarAuditoria('local', NEW.ID_Local, 'UPDATE',
      JSON_OBJECT('NM_Local', OLD.NM_Local, 'TP_Local', OLD.TP_Local,
                  'IDR_Responsavel', OLD.IDR_Responsavel, 'VL_Latitude', OLD.VL_Latitude,
                  'VL_Longitude', OLD.VL_Longitude, 'NM_Horario', OLD.NM_Horario,
                  'DS_Observacao', OLD.DS_Observacao, 'FG_Ativo', OLD.FG_Ativo,
                  'FG_Excluido', OLD.FG_Excluido,
                  'DT_Cadastro', DATE_FORMAT(OLD.DT_Cadastro, '%Y-%m-%d %H:%i:%s'),
                  'DT_Alteracao', DATE_FORMAT(OLD.DT_Alteracao, '%Y-%m-%d %H:%i:%s')),
      JSON_OBJECT('NM_Local', NEW.NM_Local, 'TP_Local', NEW.TP_Local,
                  'IDR_Responsavel', NEW.IDR_Responsavel, 'VL_Latitude', NEW.VL_Latitude,
                  'VL_Longitude', NEW.VL_Longitude, 'NM_Horario', NEW.NM_Horario,
                  'DS_Observacao', NEW.DS_Observacao, 'FG_Ativo', NEW.FG_Ativo,
                  'FG_Excluido', NEW.FG_Excluido,
                  'DT_Cadastro', DATE_FORMAT(NEW.DT_Cadastro, '%Y-%m-%d %H:%i:%s'),
                  'DT_Alteracao', DATE_FORMAT(NEW.DT_Alteracao, '%Y-%m-%d %H:%i:%s')),
      NEW.IDR_UsuarioAlteracao);
  END IF;
END$$

-- usuario (NUNCA audita a senha) ------------------------------------
CREATE TRIGGER TRG_usuario_ai_audit AFTER INSERT ON usuario FOR EACH ROW
BEGIN
  CALL SP_RegistrarAuditoria('usuario', NEW.ID_Usuario, 'INSERT', NULL,
    JSON_OBJECT('NM_Usuario', NEW.NM_Usuario, 'NM_Login', NEW.NM_Login,
                'NM_Email', NEW.NM_Email, 'IDR_Perfil', NEW.IDR_Perfil,
                'FG_Ativo', NEW.FG_Ativo, 'FG_Excluido', NEW.FG_Excluido,
                'DT_Cadastro', DATE_FORMAT(NEW.DT_Cadastro, '%Y-%m-%d %H:%i:%s'),
                'DT_Alteracao', DATE_FORMAT(NEW.DT_Alteracao, '%Y-%m-%d %H:%i:%s')),
    NEW.IDR_UsuarioCadastro);
  CALL SP_RegistrarVersionamento('usuario', NEW.ID_Usuario, 'INSERT',
    JSON_OBJECT('NM_Login', NEW.NM_Login, 'NM_Email', NEW.NM_Email, 'IDR_Perfil', NEW.IDR_Perfil, 'IDR_Empresa', NEW.IDR_Empresa),
    NEW.IDR_UsuarioCadastro);
END$$
CREATE TRIGGER TRG_usuario_au_audit AFTER UPDATE ON usuario FOR EACH ROW
BEGIN
  IF NOT (OLD.NM_Usuario <=> NEW.NM_Usuario AND OLD.NM_Login <=> NEW.NM_Login
          AND OLD.NM_Email <=> NEW.NM_Email AND OLD.IDR_Perfil <=> NEW.IDR_Perfil
          AND OLD.FG_Ativo <=> NEW.FG_Ativo AND OLD.FG_Excluido <=> NEW.FG_Excluido) THEN
    CALL SP_RegistrarAuditoria('usuario', NEW.ID_Usuario, 'UPDATE',
      JSON_OBJECT('NM_Usuario', OLD.NM_Usuario, 'NM_Login', OLD.NM_Login,
                  'NM_Email', OLD.NM_Email, 'IDR_Perfil', OLD.IDR_Perfil,
                  'FG_Ativo', OLD.FG_Ativo, 'FG_Excluido', OLD.FG_Excluido,
                  'DT_Cadastro', DATE_FORMAT(OLD.DT_Cadastro, '%Y-%m-%d %H:%i:%s'),
                  'DT_Alteracao', DATE_FORMAT(OLD.DT_Alteracao, '%Y-%m-%d %H:%i:%s')),
      JSON_OBJECT('NM_Usuario', NEW.NM_Usuario, 'NM_Login', NEW.NM_Login,
                  'NM_Email', NEW.NM_Email, 'IDR_Perfil', NEW.IDR_Perfil,
                  'FG_Ativo', NEW.FG_Ativo, 'FG_Excluido', NEW.FG_Excluido,
                  'DT_Cadastro', DATE_FORMAT(NEW.DT_Cadastro, '%Y-%m-%d %H:%i:%s'),
                  'DT_Alteracao', DATE_FORMAT(NEW.DT_Alteracao, '%Y-%m-%d %H:%i:%s')),
      NEW.IDR_UsuarioAlteracao);
    CALL SP_RegistrarVersionamento('usuario', NEW.ID_Usuario, 'UPDATE',
      JSON_OBJECT('NM_Login', NEW.NM_Login, 'NM_Email', NEW.NM_Email, 'IDR_Perfil', NEW.IDR_Perfil, 'FG_Ativo', NEW.FG_Ativo, 'FG_Excluido', NEW.FG_Excluido),
      NEW.IDR_UsuarioAlteracao);
  END IF;
END$$

-- equipe ------------------------------------------------------------
CREATE TRIGGER TRG_equipe_ai_audit AFTER INSERT ON equipe FOR EACH ROW
BEGIN
  CALL SP_RegistrarAuditoria('equipe', NEW.ID_Equipe, 'INSERT', NULL,
    JSON_OBJECT('NM_Equipe', NEW.NM_Equipe, 'TP_Equipe', NEW.TP_Equipe,
                'IDR_Local', NEW.IDR_Local, 'DS_Responsabilidade', NEW.DS_Responsabilidade,
                'FG_Ativo', NEW.FG_Ativo, 'FG_Excluido', NEW.FG_Excluido,
                'DT_Cadastro', DATE_FORMAT(NEW.DT_Cadastro, '%Y-%m-%d %H:%i:%s'),
                'DT_Alteracao', DATE_FORMAT(NEW.DT_Alteracao, '%Y-%m-%d %H:%i:%s')),
    NEW.IDR_UsuarioCadastro);
END$$
CREATE TRIGGER TRG_equipe_au_audit AFTER UPDATE ON equipe FOR EACH ROW
BEGIN
  IF NOT (OLD.NM_Equipe <=> NEW.NM_Equipe AND OLD.TP_Equipe <=> NEW.TP_Equipe
          AND OLD.IDR_Local <=> NEW.IDR_Local AND OLD.DS_Responsabilidade <=> NEW.DS_Responsabilidade
          AND OLD.FG_Ativo <=> NEW.FG_Ativo AND OLD.FG_Excluido <=> NEW.FG_Excluido) THEN
    CALL SP_RegistrarAuditoria('equipe', NEW.ID_Equipe, 'UPDATE',
      JSON_OBJECT('NM_Equipe', OLD.NM_Equipe, 'TP_Equipe', OLD.TP_Equipe,
                  'IDR_Local', OLD.IDR_Local, 'DS_Responsabilidade', OLD.DS_Responsabilidade,
                  'FG_Ativo', OLD.FG_Ativo, 'FG_Excluido', OLD.FG_Excluido,
                  'DT_Cadastro', DATE_FORMAT(OLD.DT_Cadastro, '%Y-%m-%d %H:%i:%s'),
                  'DT_Alteracao', DATE_FORMAT(OLD.DT_Alteracao, '%Y-%m-%d %H:%i:%s')),
      JSON_OBJECT('NM_Equipe', NEW.NM_Equipe, 'TP_Equipe', NEW.TP_Equipe,
                  'IDR_Local', NEW.IDR_Local, 'DS_Responsabilidade', NEW.DS_Responsabilidade,
                  'FG_Ativo', NEW.FG_Ativo, 'FG_Excluido', NEW.FG_Excluido,
                  'DT_Cadastro', DATE_FORMAT(NEW.DT_Cadastro, '%Y-%m-%d %H:%i:%s'),
                  'DT_Alteracao', DATE_FORMAT(NEW.DT_Alteracao, '%Y-%m-%d %H:%i:%s')),
      NEW.IDR_UsuarioAlteracao);
  END IF;
END$$

-- triagem -----------------------------------------------------------
CREATE TRIGGER TRG_triagem_ai_audit AFTER INSERT ON triagem FOR EACH ROW
BEGIN
  CALL SP_RegistrarAuditoria('triagem', NEW.ID_Triagem, 'INSERT', NULL,
    JSON_OBJECT('IDR_Item', NEW.IDR_Item, 'TP_Status', NEW.TP_Status,
                'DT_Cadastro', DATE_FORMAT(NEW.DT_Cadastro, '%Y-%m-%d %H:%i:%s'),
                'DT_Alteracao', DATE_FORMAT(NEW.DT_Alteracao, '%Y-%m-%d %H:%i:%s'),
                'DT_Conclusao', DATE_FORMAT(NEW.DT_Conclusao, '%Y-%m-%d %H:%i:%s')),
    NEW.IDR_UsuarioCadastro);
END$$
CREATE TRIGGER TRG_triagem_au_audit AFTER UPDATE ON triagem FOR EACH ROW
BEGIN
  IF NOT (OLD.TP_Status <=> NEW.TP_Status AND OLD.FG_Excluido <=> NEW.FG_Excluido
          AND OLD.DT_Conclusao <=> NEW.DT_Conclusao) THEN
    CALL SP_RegistrarAuditoria('triagem', NEW.ID_Triagem, 'UPDATE',
      JSON_OBJECT('TP_Status', OLD.TP_Status, 'FG_Excluido', OLD.FG_Excluido,
                  'DT_Cadastro', DATE_FORMAT(OLD.DT_Cadastro, '%Y-%m-%d %H:%i:%s'),
                  'DT_Alteracao', DATE_FORMAT(OLD.DT_Alteracao, '%Y-%m-%d %H:%i:%s'),
                  'DT_Conclusao', DATE_FORMAT(OLD.DT_Conclusao, '%Y-%m-%d %H:%i:%s')),
      JSON_OBJECT('TP_Status', NEW.TP_Status, 'FG_Excluido', NEW.FG_Excluido,
                  'DT_Cadastro', DATE_FORMAT(NEW.DT_Cadastro, '%Y-%m-%d %H:%i:%s'),
                  'DT_Alteracao', DATE_FORMAT(NEW.DT_Alteracao, '%Y-%m-%d %H:%i:%s'),
                  'DT_Conclusao', DATE_FORMAT(NEW.DT_Conclusao, '%Y-%m-%d %H:%i:%s')),
      NEW.IDR_UsuarioAlteracao);
  END IF;
END$$

DELIMITER ;
