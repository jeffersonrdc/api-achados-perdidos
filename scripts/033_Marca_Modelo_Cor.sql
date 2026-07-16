-- =====================================================================
-- 033_Marca_Modelo_Cor.sql
-- Catálogos globais (reference data) para alimentar os selects da tela de
-- Coleta/Edição de itens:
--
--   1) `cor`    -> catálogo de cores.
--   2) `marca`  -> catálogo de marcas.
--   3) `modelo` -> catálogo de modelos (FK -> marca; modelo pertence a marca).
--
-- O item CONTINUA gravando NM_Marca / NM_Modelo / NM_Cor como texto (colunas
-- já existentes). Estas tabelas apenas fornecem as opções dos selects e
-- permitem cadastrar novos valores. Nenhuma alteração destrutiva em `item`.
--
-- Aplicar com: mysql --default-character-set=utf8mb4
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) Cor
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cor (
  ID_Cor        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  NM_Cor        VARCHAR(60)   NOT NULL,
  CD_Hex        VARCHAR(7)    NULL COMMENT 'Cor em hexadecimal (#RRGGBB) para a UI',
  OR_Ordem      INT UNSIGNED  NOT NULL DEFAULT 0,
  DT_Cadastro   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  DT_Alteracao  DATETIME      NULL ON UPDATE CURRENT_TIMESTAMP,
  FG_Ativo      TINYINT(1)    NOT NULL DEFAULT 1,
  FG_Excluido   TINYINT(1)    NOT NULL DEFAULT 0,
  UNIQUE KEY UK_cor_nome (NM_Cor),
  KEY IX_cor_ordem (OR_Ordem)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Catálogo de cores';

-- ---------------------------------------------------------------------
-- 2) Marca
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS marca (
  ID_Marca      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  NM_Marca      VARCHAR(100)  NOT NULL,
  OR_Ordem      INT UNSIGNED  NOT NULL DEFAULT 0,
  DT_Cadastro   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  DT_Alteracao  DATETIME      NULL ON UPDATE CURRENT_TIMESTAMP,
  FG_Ativo      TINYINT(1)    NOT NULL DEFAULT 1,
  FG_Excluido   TINYINT(1)    NOT NULL DEFAULT 0,
  UNIQUE KEY UK_marca_nome (NM_Marca),
  KEY IX_marca_ordem (OR_Ordem)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Catálogo de marcas';

-- ---------------------------------------------------------------------
-- 3) Modelo (pertence a uma marca)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS modelo (
  ID_Modelo     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  IDR_Marca     BIGINT UNSIGNED NOT NULL,
  NM_Modelo     VARCHAR(120)  NOT NULL,
  OR_Ordem      INT UNSIGNED  NOT NULL DEFAULT 0,
  DT_Cadastro   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  DT_Alteracao  DATETIME      NULL ON UPDATE CURRENT_TIMESTAMP,
  FG_Ativo      TINYINT(1)    NOT NULL DEFAULT 1,
  FG_Excluido   TINYINT(1)    NOT NULL DEFAULT 0,
  CONSTRAINT FK_modelo_marca FOREIGN KEY (IDR_Marca) REFERENCES marca (ID_Marca),
  UNIQUE KEY UK_modelo_marca_nome (IDR_Marca, NM_Modelo),
  KEY IX_modelo_marca (IDR_Marca)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Catálogo de modelos (pertence a marca)';

-- ---------------------------------------------------------------------
-- 4) Seed: cores
-- ---------------------------------------------------------------------
INSERT INTO cor (NM_Cor, CD_Hex, OR_Ordem) VALUES
  ('Preto',    '#111827',  1),
  ('Branco',   '#F9FAFB',  2),
  ('Cinza',    '#6B7280',  3),
  ('Prata',    '#C0C0C0',  4),
  ('Dourado',  '#D4AF37',  5),
  ('Azul',     '#2563EB',  6),
  ('Vermelho', '#DC2626',  7),
  ('Verde',    '#16A34A',  8),
  ('Amarelo',  '#EAB308',  9),
  ('Rosa',     '#EC4899', 10),
  ('Roxo',     '#7C3AED', 11),
  ('Laranja',  '#F97316', 12),
  ('Marrom',   '#92400E', 13),
  ('Bege',     '#D9C7A3', 14),
  ('Transparente', NULL,  15),
  ('Outra',    NULL,      99)
ON DUPLICATE KEY UPDATE CD_Hex = VALUES(CD_Hex), OR_Ordem = VALUES(OR_Ordem);

-- ---------------------------------------------------------------------
-- 5) Seed: marcas (foco em itens comuns de achados & perdidos em evento)
-- ---------------------------------------------------------------------
INSERT INTO marca (NM_Marca, OR_Ordem) VALUES
  ('Apple', 1), ('Samsung', 2), ('Xiaomi', 3), ('Motorola', 4), ('LG', 5),
  ('Sony', 6), ('JBL', 7), ('Ray-Ban', 8), ('Oakley', 9), ('Nike', 10),
  ('Adidas', 11), ('Fossil', 12), ('Garmin', 13), ('Huawei', 14),
  ('Asus', 15), ('Dell', 16), ('Lenovo', 17), ('Outra', 99)
ON DUPLICATE KEY UPDATE OR_Ordem = VALUES(OR_Ordem);

-- ---------------------------------------------------------------------
-- 6) Seed: modelos por marca
-- ---------------------------------------------------------------------
INSERT INTO modelo (IDR_Marca, NM_Modelo, OR_Ordem)
SELECT m.ID_Marca, x.NM_Modelo, x.OR_Ordem
FROM (
  SELECT 'Apple'    AS NM_Marca, 'iPhone 11'    AS NM_Modelo, 1 AS OR_Ordem UNION ALL
  SELECT 'Apple', 'iPhone 12', 2 UNION ALL
  SELECT 'Apple', 'iPhone 13', 3 UNION ALL
  SELECT 'Apple', 'iPhone 14', 4 UNION ALL
  SELECT 'Apple', 'iPhone 15', 5 UNION ALL
  SELECT 'Apple', 'AirPods', 6 UNION ALL
  SELECT 'Apple', 'AirPods Pro', 7 UNION ALL
  SELECT 'Apple', 'Apple Watch', 8 UNION ALL
  SELECT 'Samsung', 'Galaxy S21', 1 UNION ALL
  SELECT 'Samsung', 'Galaxy S22', 2 UNION ALL
  SELECT 'Samsung', 'Galaxy S23', 3 UNION ALL
  SELECT 'Samsung', 'Galaxy A54', 4 UNION ALL
  SELECT 'Samsung', 'Galaxy Buds', 5 UNION ALL
  SELECT 'Xiaomi', 'Redmi Note 12', 1 UNION ALL
  SELECT 'Xiaomi', 'Redmi Note 13', 2 UNION ALL
  SELECT 'Xiaomi', 'Poco X5', 3 UNION ALL
  SELECT 'Xiaomi', 'Mi Band', 4 UNION ALL
  SELECT 'Motorola', 'Moto G73', 1 UNION ALL
  SELECT 'Motorola', 'Moto G84', 2 UNION ALL
  SELECT 'Motorola', 'Edge 40', 3 UNION ALL
  SELECT 'JBL', 'Go 3', 1 UNION ALL
  SELECT 'JBL', 'Flip 6', 2 UNION ALL
  SELECT 'JBL', 'Tune 510BT', 3 UNION ALL
  SELECT 'Sony', 'WH-1000XM5', 1 UNION ALL
  SELECT 'Sony', 'WF-1000XM4', 2 UNION ALL
  SELECT 'Ray-Ban', 'Aviator', 1 UNION ALL
  SELECT 'Ray-Ban', 'Wayfarer', 2 UNION ALL
  SELECT 'Garmin', 'Forerunner', 1 UNION ALL
  SELECT 'Garmin', 'Venu', 2
) x
JOIN marca m ON m.NM_Marca = x.NM_Marca
ON DUPLICATE KEY UPDATE modelo.OR_Ordem = VALUES(OR_Ordem);

-- ---------------------------------------------------------------------
-- 7) Backfill: garante que valores já usados em `item` apareçam nos selects
-- ---------------------------------------------------------------------
-- 7.1 cores existentes
INSERT INTO cor (NM_Cor, OR_Ordem)
SELECT DISTINCT TRIM(i.NM_Cor), 50
FROM item i
WHERE i.NM_Cor IS NOT NULL AND TRIM(i.NM_Cor) <> ''
ON DUPLICATE KEY UPDATE cor.NM_Cor = cor.NM_Cor;

-- 7.2 marcas existentes
INSERT INTO marca (NM_Marca, OR_Ordem)
SELECT DISTINCT TRIM(i.NM_Marca), 50
FROM item i
WHERE i.NM_Marca IS NOT NULL AND TRIM(i.NM_Marca) <> ''
ON DUPLICATE KEY UPDATE marca.NM_Marca = marca.NM_Marca;

-- 7.3 modelos existentes (associados à respectiva marca)
INSERT INTO modelo (IDR_Marca, NM_Modelo, OR_Ordem)
SELECT DISTINCT m.ID_Marca, TRIM(i.NM_Modelo), 50
FROM item i
JOIN marca m ON m.NM_Marca = TRIM(i.NM_Marca) COLLATE utf8mb4_unicode_ci
WHERE i.NM_Modelo IS NOT NULL AND TRIM(i.NM_Modelo) <> ''
  AND i.NM_Marca IS NOT NULL AND TRIM(i.NM_Marca) <> ''
ON DUPLICATE KEY UPDATE modelo.NM_Modelo = modelo.NM_Modelo;
