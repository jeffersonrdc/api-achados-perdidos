-- =====================================================================
-- 058_Email_Portal_Contato.sql
-- Parametro de e-mail para o formulario publico /contato.
-- Destinatario = NM_Remetente (ou NM_Usuario) da conta SMTP vinculada.
-- Aplicar com: mysql --default-character-set=utf8mb4 -u root achados_perdidos < 058_Email_Portal_Contato.sql
-- =====================================================================

INSERT INTO email_parametro (TP_Evento, NM_Template, NM_Assunto)
VALUES (
  'PORTAL_CONTATO',
  'portal-contato.html',
  'Contato do portal - #{{protocolo}} - {{assunto}}'
)
ON DUPLICATE KEY UPDATE
  NM_Template = VALUES(NM_Template),
  NM_Assunto  = VALUES(NM_Assunto);

-- Reaproveita a conta ja usada em CLAIM_ANALISE (se houver), para ja funcionar
-- sem exigir configuracao manual — o admin pode trocar depois em /configuracoes.
UPDATE email_parametro p
JOIN email_parametro ref ON ref.TP_Evento = 'CLAIM_ANALISE' AND ref.IDR_EmailConfig IS NOT NULL
SET p.IDR_EmailConfig = ref.IDR_EmailConfig
WHERE p.TP_Evento = 'PORTAL_CONTATO'
  AND p.IDR_EmailConfig IS NULL;
