-- =====================================================================
-- 023_Corrige_Acentos_Categoria.sql
-- Corrige nomes de categoria corrompidos na seed inicial (014), onde
-- caracteres acentuados foram gravados como caixa (ex.: "Eletr├┤nicos").
-- Aplicar com: mysql --default-character-set=utf8mb4
-- =====================================================================
UPDATE categoria SET NM_Categoria = 'Eletrônicos'          WHERE ID_Categoria = 1;
UPDATE categoria SET NM_Categoria = 'Vestuário'            WHERE ID_Categoria = 3;
UPDATE categoria SET NM_Categoria = 'Óculos e Acessórios'  WHERE ID_Categoria = 6;
