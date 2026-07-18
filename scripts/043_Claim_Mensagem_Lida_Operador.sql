-- =====================================================================
-- 043_Claim_Mensagem_Lida_Operador.sql
-- Flag de leitura pelo operador (mensagens do solicitante ainda não vistas).
-- Aplicar com: mysql --default-character-set=utf8mb4
-- =====================================================================

SET @schema_name = DATABASE();

SET @sql = IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'claim_mensagem'
      AND COLUMN_NAME = 'FG_LidaOperador'
  ),
  'SELECT ''FG_LidaOperador ja existe'' AS info',
  'ALTER TABLE claim_mensagem ADD COLUMN FG_LidaOperador TINYINT(1) NOT NULL DEFAULT 0 AFTER FG_EmailEnviado'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Operador: já "lidas". Solicitante existentes: não lidas (sinalizam no grid).
UPDATE claim_mensagem
SET FG_LidaOperador = CASE WHEN TP_Autor = 'OPERADOR' THEN 1 ELSE 0 END
WHERE FG_Excluido = 0;

SET @sql = IF(
  EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'claim_mensagem'
      AND INDEX_NAME = 'IX_claimmsg_nao_lida'
  ),
  'SELECT ''IX_claimmsg_nao_lida ja existe'' AS info',
  'ALTER TABLE claim_mensagem ADD KEY IX_claimmsg_nao_lida (IDR_Claim, TP_Autor, FG_LidaOperador, FG_Excluido)'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
