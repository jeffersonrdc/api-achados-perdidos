-- =====================================================================
-- 071_Marca_Subcategoria.sql
-- Vincula marca ↔ subcategoria (N:N), no mesmo espírito da tag:
--   tag.IDR_Subcategoria  → 1 tag pertence a 1 subcategoria
--   marca_subcategoria    → 1 marca pode pertencer a N subcategorias
--     (ex.: Apple em Smartphones, Fones, Tablets, Notebooks, Smartwatches)
--
-- Por que NÃO colocar IDR_Subcategoria direto em `marca`:
--   UNIQUE(NM_Marca) impede "Apple" em mais de uma subcategoria.
--
-- Cascade dos selects: subcategoria → marcas → (marca+sub) modelos
-- =====================================================================

CREATE TABLE IF NOT EXISTS marca_subcategoria (
  IDR_Marca         BIGINT UNSIGNED NOT NULL,
  IDR_Subcategoria  BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (IDR_Marca, IDR_Subcategoria),
  KEY IX_marca_sub_subcategoria (IDR_Subcategoria),
  CONSTRAINT FK_marca_sub_marca FOREIGN KEY (IDR_Marca) REFERENCES marca (ID_Marca),
  CONSTRAINT FK_marca_sub_subcategoria FOREIGN KEY (IDR_Subcategoria) REFERENCES categoria (ID_Categoria)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Marcas disponíveis por subcategoria (N:N)';

-- Popula a partir dos modelos já classificados (script 070)
INSERT IGNORE INTO marca_subcategoria (IDR_Marca, IDR_Subcategoria)
SELECT DISTINCT mo.IDR_Marca, mo.IDR_Subcategoria
FROM modelo mo
WHERE mo.FG_Excluido = 0
  AND mo.IDR_Subcategoria IS NOT NULL;
