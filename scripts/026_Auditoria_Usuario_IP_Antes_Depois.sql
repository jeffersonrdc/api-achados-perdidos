-- =====================================================================
-- 026_Auditoria_Usuario_IP_Antes_Depois.sql
-- Melhora a trilha de auditoria:
--   1) SP_RegistrarAuditoria passa a gravar o IP (@app_ip) e a usar o
--      usuario logado publicado pela aplicacao (@app_user_id) como fonte
--      primaria do responsavel (fallback para o parametro da trigger).
--   2) Triggers dos modulos gerenciados no painel (categoria, local,
--      usuario, equipe) passam a registrar o ANTES e o DEPOIS completos
--      (todos os campos de negocio), para saber exatamente o que mudou.
--
-- As variaveis de sessao @app_user_id e @app_ip sao definidas pela
-- aplicacao (AuditoriaContextService) no inicio de cada operacao de
-- escrita, na mesma transacao/conexao do INSERT/UPDATE/DELETE.
-- Executar uma unica vez.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) Stored procedure: grava usuario (sessao) + IP
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS SP_RegistrarAuditoria;

DELIMITER $$
CREATE PROCEDURE SP_RegistrarAuditoria(
  IN p_NM_Tabela VARCHAR(80),
  IN p_ID_Registro BIGINT UNSIGNED,
  IN p_TP_Acao VARCHAR(20),
  IN p_DS_Antes JSON,
  IN p_DS_Depois JSON,
  IN p_IDR_Usuario BIGINT UNSIGNED
)
BEGIN
  DECLARE v_usuario BIGINT UNSIGNED;
  -- prioridade: usuario logado publicado pela aplicacao; senao o passado pela trigger
  SET v_usuario = COALESCE(NULLIF(@app_user_id, 0), p_IDR_Usuario);
  IF p_NM_Tabela NOT IN ('auditoria', 'login_log', 'versionamento') THEN
    INSERT INTO auditoria (
      NM_Tabela, ID_Registro, TP_Acao, DS_Antes, DS_Depois, IDR_Usuario, IDR_UsuarioCadastro, NR_IP
    ) VALUES (
      p_NM_Tabela, p_ID_Registro, p_TP_Acao, p_DS_Antes, p_DS_Depois, v_usuario, v_usuario, NULLIF(@app_ip, '')
    );
  END IF;
END$$
DELIMITER ;

-- ---------------------------------------------------------------------
-- 2) Triggers com ANTES/DEPOIS completos
-- ---------------------------------------------------------------------
DROP TRIGGER IF EXISTS TRG_categoria_ai_audit;
DROP TRIGGER IF EXISTS TRG_categoria_au_audit;
DROP TRIGGER IF EXISTS TRG_local_ai_audit;
DROP TRIGGER IF EXISTS TRG_local_au_audit;
DROP TRIGGER IF EXISTS TRG_usuario_ai_audit;
DROP TRIGGER IF EXISTS TRG_usuario_au_audit;
DROP TRIGGER IF EXISTS TRG_equipe_ai_audit;
DROP TRIGGER IF EXISTS TRG_equipe_au_audit;

DELIMITER $$

-- categoria ---------------------------------------------------------
CREATE TRIGGER TRG_categoria_ai_audit AFTER INSERT ON categoria FOR EACH ROW
BEGIN
  CALL SP_RegistrarAuditoria('categoria', NEW.ID_Categoria, 'INSERT', NULL,
    JSON_OBJECT('NM_Categoria', NEW.NM_Categoria, 'DS_Categoria', NEW.DS_Categoria,
                'IC_Icone', NEW.IC_Icone, 'OR_Ordem', NEW.OR_Ordem,
                'FG_Ativo', NEW.FG_Ativo, 'FG_Excluido', NEW.FG_Excluido),
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
                  'FG_Ativo', OLD.FG_Ativo, 'FG_Excluido', OLD.FG_Excluido),
      JSON_OBJECT('NM_Categoria', NEW.NM_Categoria, 'DS_Categoria', NEW.DS_Categoria,
                  'IC_Icone', NEW.IC_Icone, 'OR_Ordem', NEW.OR_Ordem,
                  'FG_Ativo', NEW.FG_Ativo, 'FG_Excluido', NEW.FG_Excluido),
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
                'FG_Excluido', NEW.FG_Excluido),
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
                  'FG_Excluido', OLD.FG_Excluido),
      JSON_OBJECT('NM_Local', NEW.NM_Local, 'TP_Local', NEW.TP_Local,
                  'IDR_Responsavel', NEW.IDR_Responsavel, 'VL_Latitude', NEW.VL_Latitude,
                  'VL_Longitude', NEW.VL_Longitude, 'NM_Horario', NEW.NM_Horario,
                  'DS_Observacao', NEW.DS_Observacao, 'FG_Ativo', NEW.FG_Ativo,
                  'FG_Excluido', NEW.FG_Excluido),
      NEW.IDR_UsuarioAlteracao);
  END IF;
END$$

-- usuario (mantem o versionamento existente; NUNCA audita a senha) ---
CREATE TRIGGER TRG_usuario_ai_audit AFTER INSERT ON usuario FOR EACH ROW
BEGIN
  CALL SP_RegistrarAuditoria('usuario', NEW.ID_Usuario, 'INSERT', NULL,
    JSON_OBJECT('NM_Usuario', NEW.NM_Usuario, 'NM_Login', NEW.NM_Login,
                'NM_Email', NEW.NM_Email, 'IDR_Perfil', NEW.IDR_Perfil,
                'FG_Ativo', NEW.FG_Ativo, 'FG_Excluido', NEW.FG_Excluido),
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
                  'FG_Ativo', OLD.FG_Ativo, 'FG_Excluido', OLD.FG_Excluido),
      JSON_OBJECT('NM_Usuario', NEW.NM_Usuario, 'NM_Login', NEW.NM_Login,
                  'NM_Email', NEW.NM_Email, 'IDR_Perfil', NEW.IDR_Perfil,
                  'FG_Ativo', NEW.FG_Ativo, 'FG_Excluido', NEW.FG_Excluido),
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
                'FG_Ativo', NEW.FG_Ativo, 'FG_Excluido', NEW.FG_Excluido),
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
                  'FG_Ativo', OLD.FG_Ativo, 'FG_Excluido', OLD.FG_Excluido),
      JSON_OBJECT('NM_Equipe', NEW.NM_Equipe, 'TP_Equipe', NEW.TP_Equipe,
                  'IDR_Local', NEW.IDR_Local, 'DS_Responsabilidade', NEW.DS_Responsabilidade,
                  'FG_Ativo', NEW.FG_Ativo, 'FG_Excluido', NEW.FG_Excluido),
      NEW.IDR_UsuarioAlteracao);
  END IF;
END$$

DELIMITER ;
