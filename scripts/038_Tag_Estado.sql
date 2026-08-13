-- =====================================================================
-- 038_Tag_Estado.sql
-- Catálogos da tela /caracteristicas:
--   1) `estado` -> estados de conservação (Novo, Bom estado, Usado, Danificado).
--   2) `tag`    -> detalhes do item vinculados a uma subcategoria
--                 (ex.: Celular -> Capinha, Carregador...).
--
-- Também adiciona CRUD permissions implícitas via categoria.* (já existentes)
-- e seed inicial idempotente.
--
-- Aplicar com: mysql --default-character-set=utf8mb4
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) Estado (conservação do objeto)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS estado (
  ID_Estado     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  NM_Estado     VARCHAR(60)   NOT NULL,
  DS_Estado     VARCHAR(255)  NULL,
  OR_Ordem      INT UNSIGNED  NOT NULL DEFAULT 0,
  DT_Cadastro   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  DT_Alteracao  DATETIME      NULL ON UPDATE CURRENT_TIMESTAMP,
  FG_Ativo      TINYINT(1)    NOT NULL DEFAULT 1,
  FG_Excluido   TINYINT(1)    NOT NULL DEFAULT 0,
  UNIQUE KEY UK_estado_nome (NM_Estado),
  KEY IX_estado_ordem (OR_Ordem)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Catálogo de estados de conservação do objeto';

-- ---------------------------------------------------------------------
-- 2) Tag (detalhes vinculados à subcategoria)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tag (
  ID_Tag            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  IDR_Subcategoria  BIGINT UNSIGNED NOT NULL,
  NM_Tag            VARCHAR(100)  NOT NULL,
  DS_Tag            VARCHAR(255)  NULL,
  OR_Ordem          INT UNSIGNED  NOT NULL DEFAULT 0,
  DT_Cadastro       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  DT_Alteracao      DATETIME      NULL ON UPDATE CURRENT_TIMESTAMP,
  FG_Ativo          TINYINT(1)    NOT NULL DEFAULT 1,
  FG_Excluido       TINYINT(1)    NOT NULL DEFAULT 0,
  CONSTRAINT FK_tag_subcategoria FOREIGN KEY (IDR_Subcategoria) REFERENCES categoria (ID_Categoria),
  UNIQUE KEY UK_tag_sub_nome (IDR_Subcategoria, NM_Tag),
  KEY IX_tag_subcategoria (IDR_Subcategoria),
  KEY IX_tag_ordem (OR_Ordem)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Tags/detalhes do item vinculados a uma subcategoria';

-- ---------------------------------------------------------------------
-- 3) Seed: estados
-- ---------------------------------------------------------------------
INSERT INTO estado (NM_Estado, DS_Estado, OR_Ordem) VALUES
  ('Novo', 'Sem sinais de uso', 1),
  ('Bom estado', 'Pequenos sinais de uso, plenamente funcional', 2),
  ('Usado', 'Sinais evidentes de uso, funcional', 3),
  ('Danificado', 'Com danos que comprometem o uso', 4)
ON DUPLICATE KEY UPDATE
  DS_Estado = VALUES(DS_Estado),
  OR_Ordem = VALUES(OR_Ordem);

-- ---------------------------------------------------------------------
-- 4) Seed: tags por subcategoria (idempotente)
-- ---------------------------------------------------------------------
INSERT INTO tag (IDR_Subcategoria, NM_Tag, OR_Ordem)
SELECT c.ID_Categoria, x.NM_Tag, x.OR_Ordem
FROM (
  SELECT 'Smartphones' AS NM_Sub, 'Com capinha' AS NM_Tag, 1 AS OR_Ordem UNION ALL
  SELECT 'Smartphones', 'Com carregador', 2 UNION ALL
  SELECT 'Smartphones', 'Identificável', 3 UNION ALL
  SELECT 'Smartphones', 'Tela trincada', 4 UNION ALL
  SELECT 'Smartphones', 'Ligado / desbloqueado', 5 UNION ALL
  SELECT 'Fones de Ouvido', 'Com case', 1 UNION ALL
  SELECT 'Fones de Ouvido', 'Bluetooth', 2 UNION ALL
  SELECT 'Fones de Ouvido', 'Com fio', 3 UNION ALL
  SELECT 'Power Bank', 'Com cabo', 1 UNION ALL
  SELECT 'Power Bank', 'Carregado', 2 UNION ALL
  SELECT 'Tablets', 'Com capa', 1 UNION ALL
  SELECT 'Tablets', 'Identificável', 2 UNION ALL
  SELECT 'Notebooks', 'Com carregador', 1 UNION ALL
  SELECT 'Notebooks', 'Com capa', 2 UNION ALL
  SELECT 'Smartwatches', 'Com pulseira', 1 UNION ALL
  SELECT 'Câmeras', 'Com cartão de memória', 1 UNION ALL
  SELECT 'Câmeras', 'Com bolsa', 2 UNION ALL
  SELECT 'Carteiras', 'Com documentos', 1 UNION ALL
  SELECT 'Carteiras', 'Com cartões', 2 UNION ALL
  SELECT 'Carteiras', 'Com dinheiro', 3 UNION ALL
  SELECT 'Carteiras', 'Identificável', 4 UNION ALL
  SELECT 'Mochilas', 'Com itens dentro', 1 UNION ALL
  SELECT 'Mochilas', 'Identificável', 2 UNION ALL
  SELECT 'Bolsas', 'Com itens dentro', 1 UNION ALL
  SELECT 'Bolsas', 'Identificável', 2 UNION ALL
  SELECT 'Chaveiro', 'Com chaveiro decorativo', 1 UNION ALL
  SELECT 'Chaveiro', 'Com controle', 2 UNION ALL
  SELECT 'Chave Simples', 'Com chaveiro', 1 UNION ALL
  SELECT 'Chave de Veículo', 'Com controle', 1 UNION ALL
  SELECT 'Chave de Veículo', 'Com chaveiro', 2 UNION ALL
  SELECT 'Óculos de Grau', 'Com estojo', 1 UNION ALL
  SELECT 'Óculos de Sol', 'Com estojo', 1 UNION ALL
  SELECT 'Relógios', 'Com pulseira', 1 UNION ALL
  SELECT 'RG / CNH', 'Identificável', 1 UNION ALL
  SELECT 'Passaporte', 'Identificável', 1 UNION ALL
  SELECT 'Cartões', 'Identificável', 1 UNION ALL
  SELECT 'Camisetas', 'Com etiqueta', 1 UNION ALL
  SELECT 'Casacos', 'Com capuz', 1 UNION ALL
  SELECT 'Bonés e Chapéus', 'Com marca', 1 UNION ALL
  SELECT 'Calçados', 'Par completo', 1 UNION ALL
  SELECT 'Brinquedos', 'Infantil', 1 UNION ALL
  SELECT 'Itens Diversos', 'Identificável', 1 UNION ALL
  SELECT 'Itens Diversos', 'Sem identificação', 2 UNION ALL
  SELECT 'Itens Diversos', 'Alto valor', 3
) x
JOIN categoria c ON c.NM_Categoria = x.NM_Sub AND c.IDR_CategoriaPai IS NOT NULL AND c.FG_Excluido = 0
WHERE NOT EXISTS (
  SELECT 1 FROM tag t
  WHERE t.IDR_Subcategoria = c.ID_Categoria
    AND t.NM_Tag = x.NM_Tag
    AND t.FG_Excluido = 0
);
