-- =====================================================================
-- 029_Seed_Backfill_Itens.sql
-- Popula os itens de seed (sem os novos campos) para que a tela /itens
-- não exiba células vazias: subcategoria, estado, posto, quem encontrou,
-- usuário cadastrador (coluna Operador), modelo, prioridade e sensível.
-- Aplicar com: mysql --default-character-set=utf8mb4
-- Idempotente: só altera colunas ainda nulas / prioridade padrão.
-- =====================================================================

-- Subcategoria: primeira subcategoria (filho) da categoria do item.
UPDATE item i
JOIN (
    SELECT IDR_CategoriaPai AS pai, MIN(ID_Categoria) AS filho
    FROM categoria
    WHERE IDR_CategoriaPai IS NOT NULL AND FG_Excluido = 0
    GROUP BY IDR_CategoriaPai
) sc ON sc.pai = i.IDR_Categoria
SET i.IDR_Subcategoria = sc.filho
WHERE i.FG_Excluido = 0 AND i.IDR_Subcategoria IS NULL;

-- Usuário cadastrador (coluna Operador) — alterna entre os dois usuários.
UPDATE item SET IDR_UsuarioCadastro = CASE WHEN (ID_Item % 2) = 0 THEN 1 ELSE 2 END
WHERE FG_Excluido = 0 AND IDR_UsuarioCadastro IS NULL;

-- Quem encontrou (texto) — distribui alguns nomes de equipe.
UPDATE item SET NM_EncontradoPor = CASE (ID_Item % 5)
        WHEN 0 THEN 'João Silva'
        WHEN 1 THEN 'Maria Oliveira'
        WHEN 2 THEN 'Carlos Santos'
        WHEN 3 THEN 'Juliana Costa'
        ELSE 'Bruno Lima' END
WHERE FG_Excluido = 0 AND (NM_EncontradoPor IS NULL OR NM_EncontradoPor = '');

-- Estado de conservação.
UPDATE item SET NM_Estado = CASE (ID_Item % 3)
        WHEN 0 THEN 'Novo'
        WHEN 1 THEN 'Bom estado'
        ELSE 'Usado' END
WHERE FG_Excluido = 0 AND (NM_Estado IS NULL OR NM_Estado = '');

-- Posto / entrega.
UPDATE item SET NM_Posto = CASE (ID_Item % 4)
        WHEN 0 THEN 'Central de Achados'
        WHEN 1 THEN 'Posto Mundo 01'
        WHEN 2 THEN 'Posto Sunset 01'
        ELSE 'Posto Rock District' END
WHERE FG_Excluido = 0 AND (NM_Posto IS NULL OR NM_Posto = '');

-- Modelos para itens de marca conhecida (evita "Marca / Modelo" incompleto).
UPDATE item SET NM_Modelo = 'iPhone 13'      WHERE FG_Excluido = 0 AND NM_Marca = 'Apple'   AND (NM_Modelo IS NULL OR NM_Modelo = '');
UPDATE item SET NM_Modelo = 'Tune 510BT'     WHERE FG_Excluido = 0 AND NM_Marca = 'JBL'     AND (NM_Modelo IS NULL OR NM_Modelo = '');
UPDATE item SET NM_Modelo = 'PowerCore 10000' WHERE FG_Excluido = 0 AND NM_Marca = 'Anker'  AND (NM_Modelo IS NULL OR NM_Modelo = '');
UPDATE item SET NM_Modelo = 'Galaxy Watch 6' WHERE FG_Excluido = 0 AND NM_Marca = 'Samsung' AND (NM_Modelo IS NULL OR NM_Modelo = '');
UPDATE item SET NM_Modelo = 'Classic 1,4L'   WHERE FG_Excluido = 0 AND NM_Marca = 'Stanley' AND (NM_Modelo IS NULL OR NM_Modelo = '');
UPDATE item SET NM_Modelo = 'Aviator'        WHERE FG_Excluido = 0 AND NM_Marca = 'Ray-Ban' AND (NM_Modelo IS NULL OR NM_Modelo = '');
-- Itens sem marca conhecida recebem marca/modelo genéricos.
UPDATE item SET NM_Marca = 'Sem marca' WHERE FG_Excluido = 0 AND (NM_Marca IS NULL OR NM_Marca = '');
UPDATE item SET NM_Modelo = 'Padrão'   WHERE FG_Excluido = 0 AND (NM_Modelo IS NULL OR NM_Modelo = '');

-- Sensível: documentos, carteira, celular, smartwatch.
UPDATE item SET FG_Sensivel = 1
WHERE FG_Excluido = 0 AND (
    NM_Titulo LIKE '%iPhone%' OR NM_Titulo LIKE '%Carteira%' OR NM_Titulo LIKE '%CNH%'
 OR NM_Titulo LIKE '%Documento%' OR NM_Titulo LIKE '%Smartwatch%' OR NM_Titulo LIKE '%Motorola%');

-- Prioridade: destaca itens de valor/documentos como ALTA e triviais como BAIXA.
UPDATE item SET TP_Prioridade = 'ALTA'
WHERE FG_Excluido = 0 AND (
    NM_Titulo LIKE '%iPhone%' OR NM_Titulo LIKE '%CNH%' OR NM_Titulo LIKE '%Documento%'
 OR NM_Titulo LIKE '%Smartwatch%');
UPDATE item SET TP_Prioridade = 'BAIXA'
WHERE FG_Excluido = 0 AND (
    NM_Titulo LIKE '%Garrafa%' OR NM_Titulo LIKE '%Boné%' OR NM_Titulo LIKE '%Guarda-chuva%');
