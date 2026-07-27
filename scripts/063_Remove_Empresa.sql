-- =====================================================================
-- 063_Remove_Empresa.sql
-- Remove a entidade Empresa do modelo: não há multi-tenant real no projeto.
-- Antes: usuario.IDR_Empresa e evento.IDR_Empresa (FKs obrigatórias).
-- Também atualiza triggers de auditoria/versionamento que citavam IDR_Empresa.
-- Idempotente.
-- =====================================================================

SET @schema_name = DATABASE();

-- FK usuario -> empresa
SET @fk = (
  SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'usuario'
    AND COLUMN_NAME = 'IDR_Empresa' AND REFERENCED_TABLE_NAME = 'empresa'
  LIMIT 1
);
SET @sql = IF(@fk IS NULL, 'SELECT 1',
  CONCAT('ALTER TABLE usuario DROP FOREIGN KEY `', @fk, '`'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- FK evento -> empresa
SET @fk = (
  SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'evento'
    AND COLUMN_NAME = 'IDR_Empresa' AND REFERENCED_TABLE_NAME = 'empresa'
  LIMIT 1
);
SET @sql = IF(@fk IS NULL, 'SELECT 1',
  CONCAT('ALTER TABLE evento DROP FOREIGN KEY `', @fk, '`'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Coluna usuario.IDR_Empresa
SET @col = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'usuario' AND COLUMN_NAME = 'IDR_Empresa'
);
SET @sql = IF(@col = 0, 'SELECT 1', 'ALTER TABLE usuario DROP COLUMN IDR_Empresa');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Coluna evento.IDR_Empresa
SET @col = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'evento' AND COLUMN_NAME = 'IDR_Empresa'
);
SET @sql = IF(@col = 0, 'SELECT 1', 'ALTER TABLE evento DROP COLUMN IDR_Empresa');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

DROP TABLE IF EXISTS empresa;

-- Triggers de auditoria sem IDR_Empresa
DROP TRIGGER IF EXISTS TRG_usuario_ai_audit;
DROP TRIGGER IF EXISTS TRG_evento_ai_audit;
DROP TRIGGER IF EXISTS TRG_evento_au_audit;

DELIMITER $$

CREATE TRIGGER TRG_usuario_ai_audit
AFTER INSERT ON usuario
FOR EACH ROW
BEGIN
  CALL SP_RegistrarAuditoria('usuario', NEW.ID_Usuario, 'INSERT', NULL,
    JSON_OBJECT('NM_Usuario', NEW.NM_Usuario, 'NM_Login', NEW.NM_Login,
                'NM_Email', NEW.NM_Email, 'IDR_Perfil', NEW.IDR_Perfil,
                'FG_Ativo', NEW.FG_Ativo, 'FG_Excluido', NEW.FG_Excluido,
                'DT_Cadastro', DATE_FORMAT(NEW.DT_Cadastro, '%Y-%m-%d %H:%i:%s'),
                'DT_Alteracao', DATE_FORMAT(NEW.DT_Alteracao, '%Y-%m-%d %H:%i:%s')),
    NULL);
  CALL SP_RegistrarVersionamento('usuario', NEW.ID_Usuario, 'INSERT',
    JSON_OBJECT('NM_Login', NEW.NM_Login, 'NM_Email', NEW.NM_Email, 'IDR_Perfil', NEW.IDR_Perfil),
    NULL);
END$$

CREATE TRIGGER TRG_evento_ai_audit
AFTER INSERT ON evento
FOR EACH ROW
BEGIN
  CALL SP_RegistrarAuditoria('evento', NEW.ID_Evento, 'INSERT', NULL,
    JSON_OBJECT(
      'NM_Evento', NEW.NM_Evento, 'DS_Evento', NEW.DS_Evento,
      'DT_Inicio', DATE_FORMAT(NEW.DT_Inicio, '%Y-%m-%d %H:%i:%s'),
      'DT_Fim', DATE_FORMAT(NEW.DT_Fim, '%Y-%m-%d %H:%i:%s'),
      'NM_Local', NEW.NM_Local, 'NM_Cidade', NEW.NM_Cidade, 'SG_UF', NEW.SG_UF,
      'QT_DiasRetencao', NEW.QT_DiasRetencao, 'FG_Ativo', NEW.FG_Ativo, 'FG_Excluido', NEW.FG_Excluido,
      'DT_Cadastro', DATE_FORMAT(NEW.DT_Cadastro, '%Y-%m-%d %H:%i:%s'),
      'DT_Alteracao', DATE_FORMAT(NEW.DT_Alteracao, '%Y-%m-%d %H:%i:%s')
    ), NULL);
  CALL SP_RegistrarVersionamento('evento', NEW.ID_Evento, 'INSERT',
    JSON_OBJECT('NM_Evento', NEW.NM_Evento,
                'DT_Inicio', DATE_FORMAT(NEW.DT_Inicio, '%Y-%m-%d %H:%i:%s'),
                'DT_Fim', DATE_FORMAT(NEW.DT_Fim, '%Y-%m-%d %H:%i:%s')),
    NULL);
END$$

CREATE TRIGGER TRG_evento_au_audit
AFTER UPDATE ON evento
FOR EACH ROW
BEGIN
  IF NOT (
      OLD.NM_Evento <=> NEW.NM_Evento
      AND OLD.DS_Evento <=> NEW.DS_Evento AND OLD.DT_Inicio <=> NEW.DT_Inicio
      AND OLD.DT_Fim <=> NEW.DT_Fim
      AND OLD.NM_Local <=> NEW.NM_Local
      AND OLD.NM_Cidade <=> NEW.NM_Cidade AND OLD.SG_UF <=> NEW.SG_UF
      AND OLD.QT_DiasRetencao <=> NEW.QT_DiasRetencao
      AND OLD.FG_Ativo <=> NEW.FG_Ativo AND OLD.FG_Excluido <=> NEW.FG_Excluido
  ) THEN
    CALL SP_RegistrarAuditoria('evento', NEW.ID_Evento, 'UPDATE',
      JSON_OBJECT(
        'NM_Evento', OLD.NM_Evento, 'DS_Evento', OLD.DS_Evento,
        'DT_Inicio', DATE_FORMAT(OLD.DT_Inicio, '%Y-%m-%d %H:%i:%s'),
        'DT_Fim', DATE_FORMAT(OLD.DT_Fim, '%Y-%m-%d %H:%i:%s'),
        'NM_Local', OLD.NM_Local, 'NM_Cidade', OLD.NM_Cidade, 'SG_UF', OLD.SG_UF,
        'QT_DiasRetencao', OLD.QT_DiasRetencao, 'FG_Ativo', OLD.FG_Ativo, 'FG_Excluido', OLD.FG_Excluido,
        'DT_Cadastro', DATE_FORMAT(OLD.DT_Cadastro, '%Y-%m-%d %H:%i:%s'),
        'DT_Alteracao', DATE_FORMAT(OLD.DT_Alteracao, '%Y-%m-%d %H:%i:%s')
      ),
      JSON_OBJECT(
        'NM_Evento', NEW.NM_Evento, 'DS_Evento', NEW.DS_Evento,
        'DT_Inicio', DATE_FORMAT(NEW.DT_Inicio, '%Y-%m-%d %H:%i:%s'),
        'DT_Fim', DATE_FORMAT(NEW.DT_Fim, '%Y-%m-%d %H:%i:%s'),
        'NM_Local', NEW.NM_Local, 'NM_Cidade', NEW.NM_Cidade, 'SG_UF', NEW.SG_UF,
        'QT_DiasRetencao', NEW.QT_DiasRetencao, 'FG_Ativo', NEW.FG_Ativo, 'FG_Excluido', NEW.FG_Excluido,
        'DT_Cadastro', DATE_FORMAT(NEW.DT_Cadastro, '%Y-%m-%d %H:%i:%s'),
        'DT_Alteracao', DATE_FORMAT(NEW.DT_Alteracao, '%Y-%m-%d %H:%i:%s')
      ),
      NULL);
    CALL SP_RegistrarVersionamento('evento', NEW.ID_Evento, 'UPDATE',
      JSON_OBJECT('NM_Evento', NEW.NM_Evento, 'FG_Ativo', NEW.FG_Ativo, 'FG_Excluido', NEW.FG_Excluido),
      NULL);
  END IF;
END$$

DELIMITER ;
