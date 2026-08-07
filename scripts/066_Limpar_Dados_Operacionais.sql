-- =====================================================================
-- 066_Limpar_Dados_Operacionais.sql
-- Apaga dados operacionais/transacionais e reinicia AUTO_INCREMENT.
-- NÃO mexe em cadastros mestres (usuario, perfil, permissao, evento,
-- categoria, status, local, email_config, etc.).
--
-- Dependências extras (FKs para claim/item) também são limpas:
--   claim_pergunta, contato, item_campo, etiqueta_impressao
--
-- Uso (MySQL local / Workbench / CLI):
--   USE achados_perdidos;
--   SOURCE scripts/066_Limpar_Dados_Operacionais.sql;
-- Idempotente.
-- =====================================================================

SET @schema_name = DATABASE();
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------
-- Filhos obrigatórios (referenciam claim/item e não estavam na lista)
-- ---------------------------------------------------------------------
TRUNCATE TABLE claim_pergunta;
TRUNCATE TABLE contato;
TRUNCATE TABLE item_campo;
TRUNCATE TABLE etiqueta_impressao;

-- ---------------------------------------------------------------------
-- Devolução (folha -> raiz)
-- ---------------------------------------------------------------------
TRUNCATE TABLE devolucao_acao_token;
TRUNCATE TABLE devolucao_historico;
TRUNCATE TABLE devolucao_pickup_opcao;
TRUNCATE TABLE devolucao_shipping_cotacao;
TRUNCATE TABLE devolucao_shipping_endereco;
TRUNCATE TABLE devolucao_shipping_postagem;
TRUNCATE TABLE devolucao;

-- ---------------------------------------------------------------------
-- Claim (folha -> raiz)
-- ---------------------------------------------------------------------
TRUNCATE TABLE claim_resposta_token;
TRUNCATE TABLE claim_mensagem;
TRUNCATE TABLE claim_historico;
TRUNCATE TABLE claim_validacao;
TRUNCATE TABLE claim;

-- ---------------------------------------------------------------------
-- Item / operação de estoque
-- ---------------------------------------------------------------------
TRUNCATE TABLE item_historico;
TRUNCATE TABLE item_movimentacao;
TRUNCATE TABLE triagem;
TRUNCATE TABLE transferencia;
TRUNCATE TABLE item;

-- ---------------------------------------------------------------------
-- Arquivos, SLA, logs e trilhas
-- ---------------------------------------------------------------------
TRUNCATE TABLE arquivo;
TRUNCATE TABLE sla_registro;
TRUNCATE TABLE login_log;
TRUNCATE TABLE auth_event;
TRUNCATE TABLE auditoria;
TRUNCATE TABLE versionamento;

SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------
-- Garante AUTO_INCREMENT = 1 (TRUNCATE já faz isso no InnoDB;
-- o ALTER cobre engines/casos em que o contador não zerou)
-- ---------------------------------------------------------------------
ALTER TABLE claim_pergunta AUTO_INCREMENT = 1;
ALTER TABLE contato AUTO_INCREMENT = 1;
ALTER TABLE item_campo AUTO_INCREMENT = 1;
ALTER TABLE etiqueta_impressao AUTO_INCREMENT = 1;

ALTER TABLE devolucao_acao_token AUTO_INCREMENT = 1;
ALTER TABLE devolucao_historico AUTO_INCREMENT = 1;
ALTER TABLE devolucao_pickup_opcao AUTO_INCREMENT = 1;
ALTER TABLE devolucao_shipping_cotacao AUTO_INCREMENT = 1;
ALTER TABLE devolucao_shipping_endereco AUTO_INCREMENT = 1;
ALTER TABLE devolucao_shipping_postagem AUTO_INCREMENT = 1;
ALTER TABLE devolucao AUTO_INCREMENT = 1;

ALTER TABLE claim_resposta_token AUTO_INCREMENT = 1;
ALTER TABLE claim_mensagem AUTO_INCREMENT = 1;
ALTER TABLE claim_historico AUTO_INCREMENT = 1;
ALTER TABLE claim_validacao AUTO_INCREMENT = 1;
ALTER TABLE claim AUTO_INCREMENT = 1;

ALTER TABLE item_historico AUTO_INCREMENT = 1;
ALTER TABLE item_movimentacao AUTO_INCREMENT = 1;
ALTER TABLE triagem AUTO_INCREMENT = 1;
ALTER TABLE transferencia AUTO_INCREMENT = 1;
ALTER TABLE item AUTO_INCREMENT = 1;

ALTER TABLE arquivo AUTO_INCREMENT = 1;
ALTER TABLE sla_registro AUTO_INCREMENT = 1;
ALTER TABLE login_log AUTO_INCREMENT = 1;
ALTER TABLE auth_event AUTO_INCREMENT = 1;
ALTER TABLE auditoria AUTO_INCREMENT = 1;
ALTER TABLE versionamento AUTO_INCREMENT = 1;

-- ---------------------------------------------------------------------
-- Conferência rápida (tudo deve retornar 0)
-- ---------------------------------------------------------------------
SELECT 'arquivo' AS tabela, COUNT(*) AS qtd FROM arquivo
UNION ALL SELECT 'auditoria', COUNT(*) FROM auditoria
UNION ALL SELECT 'auth_event', COUNT(*) FROM auth_event
UNION ALL SELECT 'claim', COUNT(*) FROM claim
UNION ALL SELECT 'claim_historico', COUNT(*) FROM claim_historico
UNION ALL SELECT 'claim_mensagem', COUNT(*) FROM claim_mensagem
UNION ALL SELECT 'claim_resposta_token', COUNT(*) FROM claim_resposta_token
UNION ALL SELECT 'claim_validacao', COUNT(*) FROM claim_validacao
UNION ALL SELECT 'devolucao', COUNT(*) FROM devolucao
UNION ALL SELECT 'devolucao_acao_token', COUNT(*) FROM devolucao_acao_token
UNION ALL SELECT 'devolucao_historico', COUNT(*) FROM devolucao_historico
UNION ALL SELECT 'devolucao_pickup_opcao', COUNT(*) FROM devolucao_pickup_opcao
UNION ALL SELECT 'devolucao_shipping_cotacao', COUNT(*) FROM devolucao_shipping_cotacao
UNION ALL SELECT 'devolucao_shipping_endereco', COUNT(*) FROM devolucao_shipping_endereco
UNION ALL SELECT 'devolucao_shipping_postagem', COUNT(*) FROM devolucao_shipping_postagem
UNION ALL SELECT 'item', COUNT(*) FROM item
UNION ALL SELECT 'item_historico', COUNT(*) FROM item_historico
UNION ALL SELECT 'item_movimentacao', COUNT(*) FROM item_movimentacao
UNION ALL SELECT 'login_log', COUNT(*) FROM login_log
UNION ALL SELECT 'sla_registro', COUNT(*) FROM sla_registro
UNION ALL SELECT 'transferencia', COUNT(*) FROM transferencia
UNION ALL SELECT 'triagem', COUNT(*) FROM triagem
UNION ALL SELECT 'versionamento', COUNT(*) FROM versionamento
UNION ALL SELECT 'contato', COUNT(*) FROM contato
UNION ALL SELECT 'claim_pergunta', COUNT(*) FROM claim_pergunta
UNION ALL SELECT 'item_campo', COUNT(*) FROM item_campo
UNION ALL SELECT 'etiqueta_impressao', COUNT(*) FROM etiqueta_impressao;
