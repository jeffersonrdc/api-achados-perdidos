-- =====================================================================
-- 060_Email_Usuario_Credenciais.sql
-- Parametros de e-mail para cadastro e reset de senha de usuarios do painel.
-- Aplicar com: mysql --default-character-set=utf8mb4 -u root achados_perdidos < 060_Email_Usuario_Credenciais.sql
-- =====================================================================

INSERT INTO email_parametro (TP_Evento, NM_Template, NM_Assunto)
VALUES
  (
    'USUARIO_CADASTRO',
    'usuario-credenciais.html',
    'Seu acesso ao painel Achados e Perdidos'
  ),
  (
    'USUARIO_RESET_SENHA',
    'usuario-credenciais.html',
    'Nova senha do painel Achados e Perdidos'
  )
ON DUPLICATE KEY UPDATE
  NM_Template = VALUES(NM_Template),
  NM_Assunto  = VALUES(NM_Assunto);

-- Reaproveita a conta ja usada em CLAIM_ANALISE (se houver).
UPDATE email_parametro p
JOIN email_parametro ref ON ref.TP_Evento = 'CLAIM_ANALISE' AND ref.IDR_EmailConfig IS NOT NULL
SET p.IDR_EmailConfig = ref.IDR_EmailConfig
WHERE p.TP_Evento IN ('USUARIO_CADASTRO', 'USUARIO_RESET_SENHA')
  AND p.IDR_EmailConfig IS NULL;
