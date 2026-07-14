-- =====================================================================
-- 027_Evento_Vinculo_Processos.sql
-- Garante a regra: todo processo pertence a um evento (IDR_Evento).
--   1) lacre        -> passa a ser por evento (IDR_Evento NOT NULL + FK)
--   2) arquivo      -> IDR_Evento denormalizado (derivado da entidade referenciada)
--   3) devolucao    -> IDR_Evento (backfill via item) + FK  [trava anti-cross-event na API]
--   4) claim_validacao -> IDR_Evento (backfill via claim) + FK
--   5) contato      -> IDR_Evento (backfill via claim/item) + FK
--
-- Tabelas auxiliares (globais) permanecem SEM evento por design:
--   empresa, usuario, perfil, permissao, perfil_permissao, usuario_permissao,
--   categoria, categoria_campo, categoria_campo_opcao, status_item,
--   login_log, auditoria, versionamento.
-- Executar uma unica vez.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) lacre por evento
-- ---------------------------------------------------------------------
ALTER TABLE lacre ADD COLUMN IDR_Evento BIGINT UNSIGNED NULL AFTER ID_Lacre;
-- (numeracao NR_Lacre permanece unica globalmente; cada lacre tem seu evento)
ALTER TABLE lacre MODIFY COLUMN IDR_Evento BIGINT UNSIGNED NOT NULL;
ALTER TABLE lacre ADD CONSTRAINT FK_lacre_evento FOREIGN KEY (IDR_Evento) REFERENCES evento (ID_Evento);
ALTER TABLE lacre ADD INDEX IX_lacre_evento (IDR_Evento);

-- ---------------------------------------------------------------------
-- 2) arquivo com IDR_Evento denormalizado
-- ---------------------------------------------------------------------
ALTER TABLE arquivo ADD COLUMN IDR_Evento BIGINT UNSIGNED NULL AFTER ID_Arquivo;
-- backfill defensivo a partir das entidades referenciadas (caso existam registros)
UPDATE arquivo a JOIN item i     ON a.TP_Entidade='ITEM'    AND i.ID_Item=a.ID_Entidade    SET a.IDR_Evento=i.IDR_Evento WHERE a.IDR_Evento IS NULL;
UPDATE arquivo a JOIN claim c    ON a.TP_Entidade='CLAIM'   AND c.ID_Claim=a.ID_Entidade   SET a.IDR_Evento=c.IDR_Evento WHERE a.IDR_Evento IS NULL;
UPDATE arquivo a JOIN crianca cr ON a.TP_Entidade='CRIANCA' AND cr.ID_Crianca=a.ID_Entidade SET a.IDR_Evento=cr.IDR_Evento WHERE a.IDR_Evento IS NULL;
UPDATE arquivo a JOIN devolucao d ON d.ID_Devolucao=a.ID_Entidade JOIN item i2 ON i2.ID_Item=d.IDR_Item
       SET a.IDR_Evento=i2.IDR_Evento WHERE a.TP_Entidade='DEVOLUCAO' AND a.IDR_Evento IS NULL;
UPDATE arquivo a JOIN contato ct ON ct.ID_Contato=a.ID_Entidade JOIN claim c2 ON c2.ID_Claim=ct.IDR_Claim
       SET a.IDR_Evento=c2.IDR_Evento WHERE a.TP_Entidade='CONTATO' AND a.IDR_Evento IS NULL;
ALTER TABLE arquivo MODIFY COLUMN IDR_Evento BIGINT UNSIGNED NOT NULL;
ALTER TABLE arquivo ADD CONSTRAINT FK_arquivo_evento FOREIGN KEY (IDR_Evento) REFERENCES evento (ID_Evento);
ALTER TABLE arquivo ADD INDEX IX_arquivo_evento (IDR_Evento);

-- ---------------------------------------------------------------------
-- 3) devolucao
-- ---------------------------------------------------------------------
ALTER TABLE devolucao ADD COLUMN IDR_Evento BIGINT UNSIGNED NULL AFTER ID_Devolucao;
UPDATE devolucao d JOIN item i ON i.ID_Item = d.IDR_Item SET d.IDR_Evento = i.IDR_Evento WHERE d.IDR_Evento IS NULL;
ALTER TABLE devolucao MODIFY COLUMN IDR_Evento BIGINT UNSIGNED NOT NULL;
ALTER TABLE devolucao ADD CONSTRAINT FK_devolucao_evento FOREIGN KEY (IDR_Evento) REFERENCES evento (ID_Evento);
ALTER TABLE devolucao ADD INDEX IX_devolucao_evento (IDR_Evento);

-- ---------------------------------------------------------------------
-- 4) claim_validacao
-- ---------------------------------------------------------------------
ALTER TABLE claim_validacao ADD COLUMN IDR_Evento BIGINT UNSIGNED NULL AFTER ID_ClaimValidacao;
UPDATE claim_validacao v JOIN claim c ON c.ID_Claim = v.IDR_Claim SET v.IDR_Evento = c.IDR_Evento WHERE v.IDR_Evento IS NULL;
ALTER TABLE claim_validacao MODIFY COLUMN IDR_Evento BIGINT UNSIGNED NOT NULL;
ALTER TABLE claim_validacao ADD CONSTRAINT FK_claimvalidacao_evento FOREIGN KEY (IDR_Evento) REFERENCES evento (ID_Evento);
ALTER TABLE claim_validacao ADD INDEX IX_claimvalidacao_evento (IDR_Evento);

-- ---------------------------------------------------------------------
-- 5) contato (item e/ou claim -> evento)
-- ---------------------------------------------------------------------
ALTER TABLE contato ADD COLUMN IDR_Evento BIGINT UNSIGNED NULL AFTER ID_Contato;
UPDATE contato ct JOIN claim c ON c.ID_Claim = ct.IDR_Claim SET ct.IDR_Evento = c.IDR_Evento WHERE ct.IDR_Evento IS NULL AND ct.IDR_Claim IS NOT NULL;
UPDATE contato ct JOIN item i ON i.ID_Item = ct.IDR_Item SET ct.IDR_Evento = i.IDR_Evento WHERE ct.IDR_Evento IS NULL AND ct.IDR_Item IS NOT NULL;
ALTER TABLE contato MODIFY COLUMN IDR_Evento BIGINT UNSIGNED NOT NULL;
ALTER TABLE contato ADD CONSTRAINT FK_contato_evento FOREIGN KEY (IDR_Evento) REFERENCES evento (ID_Evento);
ALTER TABLE contato ADD INDEX IX_contato_evento (IDR_Evento);
