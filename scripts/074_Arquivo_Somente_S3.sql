-- =====================================================================
-- 074_Arquivo_Somente_S3.sql
-- Padroniza metadados: nenhum arquivo ativo fica com TP_Storage=LOCAL.
-- O binário precisa existir no S3 na key NM_Path (script Python 074 copia
-- objetos que estavam no prefixo acidental "# opcional/").
-- =====================================================================

ALTER TABLE arquivo
  MODIFY COLUMN TP_Storage VARCHAR(10) NOT NULL DEFAULT 'S3'
  COMMENT 'S3 — provedor físico deste arquivo';

UPDATE arquivo
SET TP_Storage = 'S3',
    DT_Alteracao = NOW()
WHERE TP_Storage IS NULL OR UPPER(TP_Storage) <> 'S3';

INSERT INTO sistema_parametro (NM_Chave, DS_Valor, DS_Descricao)
VALUES (
  'ARQUIVO_STORAGE_PROVIDER',
  'S3',
  'Provedor de arquivos: somente S3'
)
ON DUPLICATE KEY UPDATE
  DS_Valor = 'S3',
  DS_Descricao = 'Provedor de arquivos: somente S3';
