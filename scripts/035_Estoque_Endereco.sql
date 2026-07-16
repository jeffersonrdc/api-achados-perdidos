-- =====================================================================
-- 035_Estoque_Endereco.sql
-- Catálogo de endereçamento físico do estoque (setor, estante, prateleira,
-- caixa, posição), por depósito, para alimentar os selects da tela /estoque.
--
-- Uma única tabela com discriminador TP_Nivel + FK para o depósito. O item
-- continua gravando os nomes em `localizacao` (NM_Setor, NM_Estante, ...);
-- esta tabela apenas fornece as opções dos selects (cascade no depósito).
-- Aplicar com: mysql --default-character-set=utf8mb4
-- =====================================================================

CREATE TABLE IF NOT EXISTS estoque_endereco (
  ID_Endereco   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  IDR_Deposito  BIGINT UNSIGNED NOT NULL,
  TP_Nivel      VARCHAR(20)   NOT NULL COMMENT 'SETOR | ESTANTE | PRATELEIRA | CAIXA | POSICAO',
  NM_Endereco   VARCHAR(80)   NOT NULL,
  OR_Ordem      INT UNSIGNED  NOT NULL DEFAULT 0,
  DT_Cadastro   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  DT_Alteracao  DATETIME      NULL ON UPDATE CURRENT_TIMESTAMP,
  FG_Ativo      TINYINT(1)    NOT NULL DEFAULT 1,
  FG_Excluido   TINYINT(1)    NOT NULL DEFAULT 0,
  CONSTRAINT FK_endereco_deposito FOREIGN KEY (IDR_Deposito) REFERENCES deposito (ID_Deposito),
  UNIQUE KEY UK_endereco (IDR_Deposito, TP_Nivel, NM_Endereco),
  KEY IX_endereco_dep_nivel (IDR_Deposito, TP_Nivel)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Endereçamento físico do estoque por depósito';

-- ---------------------------------------------------------------------
-- Seed: aplica o mesmo conjunto padrão a TODOS os depósitos existentes.
-- ---------------------------------------------------------------------
INSERT INTO estoque_endereco (IDR_Deposito, TP_Nivel, NM_Endereco, OR_Ordem)
SELECT d.ID_Deposito, x.TP_Nivel, x.NM_Endereco, x.OR_Ordem
FROM deposito d
JOIN (
  SELECT 'SETOR' AS TP_Nivel, 'Setor A' AS NM_Endereco, 1 AS OR_Ordem UNION ALL
  SELECT 'SETOR','Setor B',2 UNION ALL
  SELECT 'SETOR','Setor C',3 UNION ALL
  SELECT 'SETOR','Setor D',4 UNION ALL
  SELECT 'SETOR','Setor Sensível',5 UNION ALL
  SELECT 'SETOR','Setor Restrito',6 UNION ALL
  SELECT 'ESTANTE','Estante 01',1 UNION ALL
  SELECT 'ESTANTE','Estante 02',2 UNION ALL
  SELECT 'ESTANTE','Estante 03',3 UNION ALL
  SELECT 'ESTANTE','Estante 04',4 UNION ALL
  SELECT 'ESTANTE','Estante 05',5 UNION ALL
  SELECT 'ESTANTE','Armário Seguro',6 UNION ALL
  SELECT 'PRATELEIRA','P1',1 UNION ALL
  SELECT 'PRATELEIRA','P2',2 UNION ALL
  SELECT 'PRATELEIRA','P3',3 UNION ALL
  SELECT 'PRATELEIRA','P4',4 UNION ALL
  SELECT 'PRATELEIRA','P5',5 UNION ALL
  SELECT 'PRATELEIRA','P6',6 UNION ALL
  SELECT 'CAIXA','Caixa ELET-015',1 UNION ALL
  SELECT 'CAIXA','Caixa ELET-022',2 UNION ALL
  SELECT 'CAIXA','Caixa BOLS-008',3 UNION ALL
  SELECT 'CAIXA','Caixa DOC-003',4 UNION ALL
  SELECT 'CAIXA','Caixa ACESS-011',5 UNION ALL
  SELECT 'CAIXA','Caixa OUT-001',6 UNION ALL
  SELECT 'POSICAO','01',1 UNION ALL
  SELECT 'POSICAO','02',2 UNION ALL
  SELECT 'POSICAO','03',3 UNION ALL
  SELECT 'POSICAO','04',4 UNION ALL
  SELECT 'POSICAO','05',5 UNION ALL
  SELECT 'POSICAO','06',6 UNION ALL
  SELECT 'POSICAO','07',7 UNION ALL
  SELECT 'POSICAO','08',8 UNION ALL
  SELECT 'POSICAO','09',9 UNION ALL
  SELECT 'POSICAO','10',10
) x
WHERE d.FG_Excluido = 0
ON DUPLICATE KEY UPDATE estoque_endereco.OR_Ordem = VALUES(OR_Ordem);

-- ---------------------------------------------------------------------
-- Backfill: garante que valores já usados em `localizacao` estejam nos selects.
-- ---------------------------------------------------------------------
INSERT INTO estoque_endereco (IDR_Deposito, TP_Nivel, NM_Endereco, OR_Ordem)
SELECT IDR_Deposito, TP_Nivel, NM_Endereco, 50 FROM (
  SELECT IDR_Deposito, 'SETOR' AS TP_Nivel, TRIM(NM_Setor) AS NM_Endereco
    FROM localizacao WHERE FG_Excluido = 0 AND NM_Setor IS NOT NULL AND TRIM(NM_Setor) <> ''
  UNION
  SELECT IDR_Deposito, 'ESTANTE', TRIM(NM_Estante)
    FROM localizacao WHERE FG_Excluido = 0 AND NM_Estante IS NOT NULL AND TRIM(NM_Estante) <> ''
  UNION
  SELECT IDR_Deposito, 'PRATELEIRA', TRIM(NM_Prateleira)
    FROM localizacao WHERE FG_Excluido = 0 AND NM_Prateleira IS NOT NULL AND TRIM(NM_Prateleira) <> ''
  UNION
  SELECT IDR_Deposito, 'CAIXA', TRIM(NM_Caixa)
    FROM localizacao WHERE FG_Excluido = 0 AND NM_Caixa IS NOT NULL AND TRIM(NM_Caixa) <> ''
  UNION
  SELECT IDR_Deposito, 'POSICAO', TRIM(NM_Posicao)
    FROM localizacao WHERE FG_Excluido = 0 AND NM_Posicao IS NOT NULL AND TRIM(NM_Posicao) <> ''
) v
ON DUPLICATE KEY UPDATE estoque_endereco.NM_Endereco = estoque_endereco.NM_Endereco;
