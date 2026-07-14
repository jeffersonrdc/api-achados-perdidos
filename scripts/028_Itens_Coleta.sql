-- =====================================================================
-- 028_Itens_Coleta.sql
-- Integração da tela /itens (Coleta) com a API.
--   1) categoria  -> hierarquia pai/filho (IDR_CategoriaPai + FK) e seed de
--                    subcategorias para as 10 categorias existentes.
--   2) item       -> campos extras coletados no formulário e ainda não
--                    persistidos: NM_Estado, NM_Posto, DS_Observacoes.
-- Fotos reutilizam a tabela `arquivo` (TP_Entidade='ITEM', TP_Arquivo='FOTO').
-- Aplicar com: mysql --default-character-set=utf8mb4
-- Observação: NM_Categoria é UNIQUE -> nomes de subcategoria são únicos.
-- Executar uma única vez (os seeds usam WHERE NOT EXISTS e podem repetir).
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) categoria: hierarquia pai/filho
-- ---------------------------------------------------------------------
ALTER TABLE categoria ADD COLUMN IDR_CategoriaPai BIGINT UNSIGNED NULL AFTER NM_Categoria;
ALTER TABLE categoria ADD CONSTRAINT FK_categoria_pai FOREIGN KEY (IDR_CategoriaPai) REFERENCES categoria (ID_Categoria);
ALTER TABLE categoria ADD INDEX IX_categoria_pai (IDR_CategoriaPai);

-- Seed de subcategorias (filhos). Cada linha vincula-se ao pai pelo nome,
-- tornando o script robusto a diferenças de ID entre ambientes.
INSERT INTO categoria (NM_Categoria, IDR_CategoriaPai, OR_Ordem, FG_Ativo, FG_Excluido, DT_Cadastro)
SELECT x.nm, p.ID_Categoria, x.ordem, 1, 0, NOW()
FROM (
    -- Eletrônicos
    SELECT 'Eletrônicos' AS pai, 'Smartphones'          AS nm, 1 AS ordem UNION ALL
    SELECT 'Eletrônicos', 'Fones de Ouvido',      2 UNION ALL
    SELECT 'Eletrônicos', 'Power Bank',            3 UNION ALL
    SELECT 'Eletrônicos', 'Tablets',               4 UNION ALL
    SELECT 'Eletrônicos', 'Notebooks',             5 UNION ALL
    SELECT 'Eletrônicos', 'Smartwatches',          6 UNION ALL
    SELECT 'Eletrônicos', 'Câmeras',               7 UNION ALL
    SELECT 'Eletrônicos', 'Outros Eletrônicos',    8 UNION ALL
    -- Documentos
    SELECT 'Documentos',  'RG / CNH',              1 UNION ALL
    SELECT 'Documentos',  'Passaporte',            2 UNION ALL
    SELECT 'Documentos',  'Cartões',               3 UNION ALL
    SELECT 'Documentos',  'Crachás',               4 UNION ALL
    SELECT 'Documentos',  'Outros Documentos',     5 UNION ALL
    -- Vestuário
    SELECT 'Vestuário',   'Camisetas',             1 UNION ALL
    SELECT 'Vestuário',   'Casacos',               2 UNION ALL
    SELECT 'Vestuário',   'Bonés e Chapéus',       3 UNION ALL
    SELECT 'Vestuário',   'Calçados',              4 UNION ALL
    SELECT 'Vestuário',   'Outros Vestuário',      5 UNION ALL
    -- Bolsas e Mochilas
    SELECT 'Bolsas e Mochilas', 'Mochilas',        1 UNION ALL
    SELECT 'Bolsas e Mochilas', 'Carteiras',       2 UNION ALL
    SELECT 'Bolsas e Mochilas', 'Bolsas',          3 UNION ALL
    SELECT 'Bolsas e Mochilas', 'Necessaires',     4 UNION ALL
    SELECT 'Bolsas e Mochilas', 'Malas',           5 UNION ALL
    -- Chaves
    SELECT 'Chaves',      'Chave Simples',         1 UNION ALL
    SELECT 'Chaves',      'Chaveiro',              2 UNION ALL
    SELECT 'Chaves',      'Chave de Veículo',      3 UNION ALL
    -- Óculos e Acessórios
    SELECT 'Óculos e Acessórios', 'Óculos de Grau',      1 UNION ALL
    SELECT 'Óculos e Acessórios', 'Óculos de Sol',       2 UNION ALL
    SELECT 'Óculos e Acessórios', 'Relógios',            3 UNION ALL
    SELECT 'Óculos e Acessórios', 'Joias e Bijuterias',  4 UNION ALL
    SELECT 'Óculos e Acessórios', 'Outros Acessórios',   5 UNION ALL
    -- Esportes
    SELECT 'Esportes',    'Equipamentos Esportivos', 1 UNION ALL
    SELECT 'Esportes',    'Roupas Esportivas',       2 UNION ALL
    SELECT 'Esportes',    'Outros Esportes',         3 UNION ALL
    -- Infantil
    SELECT 'Infantil',    'Brinquedos',            1 UNION ALL
    SELECT 'Infantil',    'Roupas Infantis',       2 UNION ALL
    SELECT 'Infantil',    'Acessórios Infantis',   3 UNION ALL
    SELECT 'Infantil',    'Outros Infantil',       4 UNION ALL
    -- Alimentos
    SELECT 'Alimentos',   'Alimentos Diversos',    1 UNION ALL
    -- Outros
    SELECT 'Outros',      'Itens Diversos',        1
) AS x
JOIN categoria p ON p.NM_Categoria = x.pai AND p.IDR_CategoriaPai IS NULL AND p.FG_Excluido = 0
WHERE NOT EXISTS (
    SELECT 1 FROM categoria c WHERE c.NM_Categoria = x.nm
);

-- ---------------------------------------------------------------------
-- 2) item: campos extras do formulário de coleta
-- ---------------------------------------------------------------------
ALTER TABLE item ADD COLUMN NM_Estado      VARCHAR(40)  NULL AFTER NM_Cor;
ALTER TABLE item ADD COLUMN NM_Posto       VARCHAR(200) NULL AFTER NM_LocalEncontrado;
ALTER TABLE item ADD COLUMN DS_Observacoes TEXT         NULL AFTER DS_Item;
