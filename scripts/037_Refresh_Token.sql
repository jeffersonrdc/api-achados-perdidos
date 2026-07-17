-- =====================================================================
-- 037_Refresh_Token.sql
-- Revogação de refresh tokens (A07 — logout real e rotação de token).
--
-- Cada refresh token emitido guarda seu identificador único (jti). O logout
-- e a rotação marcam FG_Revogado=1; o endpoint /auth/refresh só aceita um
-- token cujo jti esteja registrado, não revogado e não expirado.
-- Aplicar com: mysql --default-character-set=utf8mb4
-- =====================================================================

CREATE TABLE IF NOT EXISTS refresh_token (
  ID_RefreshToken BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  NM_Jti          VARCHAR(64)     NOT NULL COMMENT 'Identificador único do token (claim jti)',
  IDR_Usuario     BIGINT UNSIGNED NOT NULL,
  DT_Expiracao    DATETIME        NOT NULL,
  FG_Revogado     TINYINT(1)      NOT NULL DEFAULT 0,
  DT_Revogacao    DATETIME        NULL,
  DT_Cadastro     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY UK_refreshtoken_jti (NM_Jti),
  KEY IX_refreshtoken_usuario (IDR_Usuario),
  KEY IX_refreshtoken_expiracao (DT_Expiracao),
  CONSTRAINT FK_refreshtoken_usuario FOREIGN KEY (IDR_Usuario) REFERENCES usuario (ID_Usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Refresh tokens emitidos, para revogação e rotação';
