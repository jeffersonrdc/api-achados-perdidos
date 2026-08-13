-- =====================================================================
-- 068_Modelo_Outro_Todas_Marcas.sql
-- Garante o modelo "Outro" para todas as marcas ativas (não excluídas).
-- Idempotente: não duplica se já existir (mesmo nome, case-insensitive).
-- Executar uma única vez (ou reaplicar com segurança).
-- =====================================================================

INSERT INTO modelo (IDR_Marca, NM_Modelo, OR_Ordem, FG_Ativo, FG_Excluido, DT_Cadastro)
SELECT m.ID_Marca, 'Outro', 9999, 1, 0, NOW()
FROM marca m
WHERE m.FG_Excluido = 0
  AND NOT EXISTS (
        SELECT 1
          FROM modelo x
         WHERE x.IDR_Marca = m.ID_Marca
           AND x.FG_Excluido = 0
           AND LOWER(x.NM_Modelo) = 'outro'
      );
