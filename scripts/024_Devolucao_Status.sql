-- =====================================================================
-- 024_Devolucao_Status.sql
-- Adiciona o status de workflow da devolução (conferência → assinatura →
-- baixa), conforme a seção 8 da especificação. Executar uma única vez.
--   AGUARDANDO_RETIRADA | EM_CONFERENCIA | AGUARDANDO_ASSINATURA | ASSINADO | CONCLUIDO
-- =====================================================================
ALTER TABLE devolucao
  ADD COLUMN TP_Status VARCHAR(30) NOT NULL DEFAULT 'AGUARDANDO_RETIRADA' AFTER FG_Concluido;

UPDATE devolucao
  SET TP_Status = CASE
    WHEN FG_Concluido = 1 THEN 'CONCLUIDO'
    WHEN FG_Assinado  = 1 THEN 'ASSINADO'
    ELSE 'AGUARDANDO_RETIRADA'
  END;
