-- =====================================================================
-- 045_Auth_Event_Logs.sql
-- 1) Garante SP_RegistrarAuditoria com @app_user_id / @app_ip
-- 2) Cria tabela append-only auth_event (trilha de acessos)
-- 3) Permissão logs.consultar (+ vínculo a perfis administrativos)
-- =====================================================================

SET @schema_name = DATABASE();

-- ---------------------------------------------------------------------
-- 1) Stored procedure de auditoria operacional
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
  SET v_usuario = COALESCE(NULLIF(@app_user_id, 0), p_IDR_Usuario);
  IF p_NM_Tabela NOT IN ('auditoria', 'login_log', 'versionamento', 'auth_event') THEN
    INSERT INTO auditoria (
      NM_Tabela, ID_Registro, TP_Acao, DS_Antes, DS_Depois, IDR_Usuario, IDR_UsuarioCadastro, NR_IP
    ) VALUES (
      p_NM_Tabela, p_ID_Registro, p_TP_Acao, p_DS_Antes, p_DS_Depois, v_usuario, v_usuario, NULLIF(@app_ip, '')
    );
  END IF;
END$$
DELIMITER ;

-- ---------------------------------------------------------------------
-- 2) Tabela auth_event (append-only)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS auth_event (
  ID_AuthEvent           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  IDR_Usuario            BIGINT UNSIGNED NULL,
  TP_Evento              VARCHAR(40)  NOT NULL COMMENT 'LOGIN_SUCESSO | LOGIN_CREDENCIAL_INVALIDA | LOGIN_RATE_LIMIT_IP | LOGIN_RATE_LIMIT_CONTA | REFRESH_SUCESSO | REFRESH_INVALIDO | LOGOUT',
  TP_Resultado           VARCHAR(20)  NOT NULL COMMENT 'SUCESSO | FALHA | BLOQUEIO',
  CD_Motivo              VARCHAR(80)  NULL,
  DS_IdentificadorMascarado VARCHAR(120) NULL,
  NR_IP                  VARCHAR(45)  NULL,
  NM_Dispositivo         VARCHAR(150) NULL,
  NM_Navegador           VARCHAR(150) NULL,
  DT_Evento              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FG_Excluido            TINYINT(1)   NOT NULL DEFAULT 0,
  CONSTRAINT FK_authevent_usuario FOREIGN KEY (IDR_Usuario) REFERENCES usuario (ID_Usuario) ON DELETE SET NULL,
  KEY IX_authevent_dt (DT_Evento),
  KEY IX_authevent_evento_dt (TP_Evento, DT_Evento),
  KEY IX_authevent_usuario_dt (IDR_Usuario, DT_Evento),
  KEY IX_authevent_ip_dt (NR_IP, DT_Evento)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Trilha append-only de eventos de autenticação/acesso';

-- ---------------------------------------------------------------------
-- 3) Permissão logs.consultar (API) — logs.acessar já existe para o menu
-- ---------------------------------------------------------------------
INSERT INTO permissao (NM_Permissao, NM_Modulo, NM_Acao, DS_Permissao, FG_Ativo, FG_Excluido)
VALUES
 ('logs.consultar', 'logs', 'consultar', 'Consultar logs de acesso (auth_event)', 1, 0)
ON DUPLICATE KEY UPDATE
 NM_Modulo = VALUES(NM_Modulo),
 NM_Acao = VALUES(NM_Acao),
 DS_Permissao = VALUES(DS_Permissao),
 FG_Ativo = 1,
 FG_Excluido = 0;

-- Vincula logs.consultar a perfis que já têm auditoria.consultar ou logs.acessar
INSERT INTO perfil_permissao (IDR_Perfil, IDR_Permissao, FG_Ativo, FG_Excluido)
SELECT DISTINCT pp.IDR_Perfil, peLogs.ID_Permissao, 1, 0
FROM perfil_permissao pp
JOIN permissao peExistente ON peExistente.ID_Permissao = pp.IDR_Permissao
CROSS JOIN permissao peLogs
WHERE peLogs.NM_Permissao = 'logs.consultar'
  AND peExistente.NM_Permissao IN ('auditoria.consultar', 'logs.acessar')
  AND pp.FG_Excluido = 0
  AND NOT EXISTS (
    SELECT 1 FROM perfil_permissao x
    WHERE x.IDR_Perfil = pp.IDR_Perfil AND x.IDR_Permissao = peLogs.ID_Permissao
  );
