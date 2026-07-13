-- =====================================================================
-- 025_Seed_Claim_Contato.sql
-- Popula dados de contato (CPF, telefone) e descrição detalhada
-- (DS_Objeto) dos claims abertos, para o painel de detalhe de
-- "Solicitações de Retirada" (paineladmin) exibir informações completas.
-- Idempotente: só preenche onde está NULL/vazio.
-- =====================================================================

-- Mochila preta com notebook (Maria Souza)
UPDATE claim SET
    NR_CPF      = COALESCE(NULLIF(NR_CPF, ''), '12345678901'),
    NR_Telefone = COALESCE(NULLIF(NR_Telefone, ''), '21999998888'),
    NM_Whatsapp = COALESCE(NULLIF(NM_Whatsapp, ''), '21999998888'),
    DS_Objeto   = COALESCE(NULLIF(DS_Objeto, ''),
        'Mochila preta modelo executivo, com notebook Dell prata dentro, carregador e um caderno vermelho. Zíper frontal com chaveiro de coruja.')
WHERE NM_Objeto LIKE 'Mochila%' AND IDR_Status = (SELECT ID_Status FROM status_item WHERE NM_Status = 'Claim Aberto');

-- Carteira marrom com documentos (Carlos Santos)
UPDATE claim SET
    NR_CPF      = COALESCE(NULLIF(NR_CPF, ''), '98765432100'),
    NR_Telefone = COALESCE(NULLIF(NR_Telefone, ''), '21988776655'),
    NM_Whatsapp = COALESCE(NULLIF(NM_Whatsapp, ''), '21988776655'),
    DS_Objeto   = COALESCE(NULLIF(DS_Objeto, ''),
        'Carteira de couro marrom com CNH, dois cartões de crédito e cerca de R$ 45 em dinheiro. Costura desgastada no canto direito.')
WHERE NM_Objeto LIKE 'Carteira%' AND IDR_Status = (SELECT ID_Status FROM status_item WHERE NM_Status = 'Claim Aberto');

-- Fone de ouvido sem fio (Ana Lima)
UPDATE claim SET
    NR_CPF      = COALESCE(NULLIF(NR_CPF, ''), '45678912300'),
    NR_Telefone = COALESCE(NULLIF(NR_Telefone, ''), '21977665544'),
    NM_Whatsapp = COALESCE(NULLIF(NM_Whatsapp, ''), '21977665544'),
    DS_Objeto   = COALESCE(NULLIF(DS_Objeto, ''),
        'Fone de ouvido sem fio branco, estojo com um adesivo de banda de rock na tampa. Um dos fones tem um risco pequeno.')
WHERE NM_Objeto LIKE 'Fone%' AND IDR_Status = (SELECT ID_Status FROM status_item WHERE NM_Status = 'Claim Aberto');

-- iPhone 13 Azul (João Pereira / Joao Lima)
UPDATE claim SET
    NR_CPF      = COALESCE(NULLIF(NR_CPF, ''), '32165498700'),
    NR_Telefone = COALESCE(NULLIF(NR_Telefone, ''), '21966554433'),
    NM_Whatsapp = COALESCE(NULLIF(NM_Whatsapp, ''), '21966554433'),
    DS_Objeto   = COALESCE(NULLIF(DS_Objeto, ''),
        'iPhone 13 azul, 128GB, capinha transparente com película de vidro trincada no canto superior direito. Papel de parede de praia ao pôr do sol.')
WHERE NM_Objeto LIKE 'iPhone%' AND IDR_Status = (SELECT ID_Status FROM status_item WHERE NM_Status = 'Claim Aberto');

-- Óculos Ray-Ban (Beatriz Alves)
UPDATE claim SET
    NR_CPF      = COALESCE(NULLIF(NR_CPF, ''), '78912345600'),
    NR_Telefone = COALESCE(NULLIF(NR_Telefone, ''), '21955443322'),
    NM_Whatsapp = COALESCE(NULLIF(NM_Whatsapp, ''), '21955443322'),
    DS_Objeto   = COALESCE(NULLIF(DS_Objeto, ''),
        'Óculos Ray-Ban wayfarer preto, com case rígido marrom e flanela de limpeza. Pequeno arranhão na lente esquerda.')
WHERE NM_Objeto LIKE '%culos%' AND IDR_Status = (SELECT ID_Status FROM status_item WHERE NM_Status = 'Claim Aberto');
