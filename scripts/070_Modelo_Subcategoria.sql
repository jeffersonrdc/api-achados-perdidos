-- =====================================================================
-- 070_Modelo_Subcategoria.sql
-- Vincula modelos a uma subcategoria (opcional) para o cascade do select:
--   marca + subcategoria → modelos relevantes.
-- IDR_Subcategoria NULL = genérico (ex.: "Outro") — aparece em qualquer subcategoria.
-- =====================================================================

-- Coluna (idempotente)
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'modelo'
    AND COLUMN_NAME = 'IDR_Subcategoria'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE modelo ADD COLUMN IDR_Subcategoria BIGINT UNSIGNED NULL COMMENT ''Subcategoria sugerida do modelo (NULL = genérico / todas)'' AFTER IDR_Marca',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- FK (idempotente)
SET @fk_exists := (
  SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'modelo'
    AND CONSTRAINT_NAME = 'FK_modelo_subcategoria'
);
SET @sql_fk := IF(@fk_exists = 0,
  'ALTER TABLE modelo ADD CONSTRAINT FK_modelo_subcategoria FOREIGN KEY (IDR_Subcategoria) REFERENCES categoria (ID_Categoria)',
  'SELECT 1');
PREPARE stmt_fk FROM @sql_fk; EXECUTE stmt_fk; DEALLOCATE PREPARE stmt_fk;

-- Índice (idempotente)
SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'modelo'
    AND INDEX_NAME = 'IX_modelo_subcategoria'
);
SET @sql_idx := IF(@idx_exists = 0,
  'ALTER TABLE modelo ADD KEY IX_modelo_subcategoria (IDR_Subcategoria)',
  'SELECT 1');
PREPARE stmt_idx FROM @sql_idx; EXECUTE stmt_idx; DEALLOCATE PREPARE stmt_idx;

-- Classificação do seed por padrão de nome (mais específico primeiro via CASE)
UPDATE modelo mo
SET mo.IDR_Subcategoria = (
  SELECT c.ID_Categoria
  FROM categoria c
  WHERE c.IDR_CategoriaPai IS NOT NULL
    AND c.FG_Excluido = 0
    AND c.NM_Categoria = CASE
      WHEN LOWER(mo.NM_Modelo) REGEXP
        'airpods|buds|powerbeats|solo |studio buds|fit pro|quietcomfort|ultra open|tone free|freebuds|linkbuds|^wf-|^wh-|tune|wave buds|live 660|soundcore|fone '
        THEN 'Fones de Ouvido'
      WHEN LOWER(mo.NM_Modelo) REGEXP
        'apple watch|galaxy watch|watch gt|mi band|redmi watch|amazfit|gtr |gts |bip |band [0-9]|fenix|forerunner|instinct|vivoactive|^venu|fossil gen'
        THEN 'Smartwatches'
      WHEN LOWER(mo.NM_Modelo) REGEXP 'ipad|galaxy tab|tab m|tablet'
        THEN 'Tablets'
      WHEN LOWER(mo.NM_Modelo) REGEXP
        'macbook|aspire|nitro|predator|zephyrus|vivobook|inspiron|latitude|xps |elitebook|pavilion|victus|ideapad|legion|thinkpad'
        THEN 'Notebooks'
      WHEN LOWER(mo.NM_Modelo) REGEXP 'eos |powershot|coolpix|^d[0-9]|^z[0-9]|alpha |hero [0-9]'
        THEN 'Câmeras'
      WHEN LOWER(mo.NM_Modelo) REGEXP 'powercore|power bank'
        THEN 'Power Bank'
      WHEN LOWER(mo.NM_Modelo) REGEXP
        'iphone|galaxy [asz]|galaxy note|xperia|pixel|redmi|poco |moto |edge |razr|zenfone|rog phone|realme |k62|velvet|smartphone|15t'
        THEN 'Smartphones'
      WHEN LOWER(mo.NM_Modelo) REGEXP
        'charge |clip |flip |boombox|go [0-9]|soundlink'
        THEN 'Outros Eletrônicos'
      WHEN LOWER(mo.NM_Modelo) REGEXP 'edifice|g-shock|vintage|grant|machine'
        THEN 'Relógios'
      WHEN LOWER(mo.NM_Modelo) REGEXP
        'aviador|aviat|quadrado|redondo|flak|frogskins|holbrook|radar|wayfarer|clubmaster|hexagonal|justin|round'
        THEN 'Óculos de Sol'
      WHEN LOWER(mo.NM_Modelo) REGEXP 'bon[eé]|59fifty|9fifty|9forty|archive'
        THEN 'Bonés e Chapéus'
      WHEN LOWER(mo.NM_Modelo) REGEXP 'mochila|shoulder|pochete'
        THEN 'Mochilas'
      WHEN LOWER(mo.NM_Modelo) REGEXP 'classic 1'
        THEN 'Itens Diversos'
      ELSE NULL
    END
  LIMIT 1
)
WHERE mo.FG_Excluido = 0
  AND LOWER(mo.NM_Modelo) <> 'outro';

UPDATE modelo
   SET IDR_Subcategoria = NULL
 WHERE LOWER(NM_Modelo) = 'outro';
