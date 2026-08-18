-- =====================================================================
-- 072_Devolucao_Rapida_Recibo.sql
-- Template de e-mail da devolução rápida (retirada presencial no evento).
-- Também cria a permissão da tela /devolucao-rapida.
-- Aplicar com: mysql --default-character-set=utf8mb4 -u root achados_perdidos < 072_Devolucao_Rapida_Recibo.sql
-- =====================================================================

INSERT INTO email_parametro (TP_Evento, NM_Template, NM_Assunto)
VALUES (
  'DEVOLUCAO_RAPIDA_RECIBO',
  'devolucao-rapida-recibo.html',
  'Recibo de retirada presencial — #{{protocolo}}'
)
ON DUPLICATE KEY UPDATE
  NM_Template = VALUES(NM_Template),
  NM_Assunto  = VALUES(NM_Assunto);

UPDATE email_parametro p
JOIN email_parametro ref ON ref.TP_Evento IN ('DEVOLUCAO_CONCLUIDA', 'CLAIM_APROVACAO', 'CLAIM_ANALISE')
                      AND ref.IDR_EmailConfig IS NOT NULL
SET p.IDR_EmailConfig = ref.IDR_EmailConfig
WHERE p.TP_Evento = 'DEVOLUCAO_RAPIDA_RECIBO'
  AND p.IDR_EmailConfig IS NULL;

INSERT INTO permissao (NM_Permissao, NM_Modulo, NM_Acao, DS_Permissao, FG_Ativo, FG_Excluido)
VALUES (
  'devolucao-rapida.acessar',
  'devolucao-rapida',
  'acessar',
  'Acessar a tela Devolução Rápida',
  1,
  0
)
ON DUPLICATE KEY UPDATE
  NM_Modulo = VALUES(NM_Modulo),
  NM_Acao = VALUES(NM_Acao),
  DS_Permissao = VALUES(DS_Permissao),
  FG_Ativo = 1,
  FG_Excluido = 0;

INSERT INTO perfil_permissao (IDR_Perfil, IDR_Permissao, FG_Ativo, FG_Excluido)
SELECT p.ID_Perfil, pe.ID_Permissao, 1, 0
FROM perfil p
CROSS JOIN permissao pe
WHERE p.FG_Ativo = 1 AND p.FG_Excluido = 0
  AND pe.NM_Permissao = 'devolucao-rapida.acessar'
  AND NOT EXISTS (
    SELECT 1 FROM perfil_permissao pp
    WHERE pp.IDR_Perfil = p.ID_Perfil AND pp.IDR_Permissao = pe.ID_Permissao
  );
