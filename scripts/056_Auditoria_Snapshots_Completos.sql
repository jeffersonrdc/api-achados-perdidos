-- =====================================================================
-- 056_Auditoria_Snapshots_Completos.sql
-- Reescreve as triggers de auditoria para gravar o snapshot COMPLETO
-- (antes/depois) dos campos de negócio — não apenas 2–3 campos.
-- Preserva side-effects: versionamento, SLA (item/claim/devolucao).
-- NÃO audita senha (usuario.NM_Senha).
-- Executar uma única vez.
-- =====================================================================

DELIMITER $$

-- ---------------------------------------------------------------------
-- ITEM
-- ---------------------------------------------------------------------
DROP TRIGGER IF EXISTS TRG_item_ai_audit$$
DROP TRIGGER IF EXISTS TRG_item_au_audit$$

CREATE TRIGGER TRG_item_ai_audit AFTER INSERT ON item FOR EACH ROW
BEGIN
  CALL SP_RegistrarAuditoria('item', NEW.ID_Item, 'INSERT', NULL,
    JSON_OBJECT(
      'IDR_Evento', NEW.IDR_Evento,
      'IDR_Categoria', NEW.IDR_Categoria,
      'IDR_Subcategoria', NEW.IDR_Subcategoria,
      'IDR_Localizacao', NEW.IDR_Localizacao,
      'IDR_LocalAtual', NEW.IDR_LocalAtual,
      'IDR_Status', NEW.IDR_Status,
      'IDR_Lacre', NEW.IDR_Lacre,
      'CD_Item', NEW.CD_Item,
      'NR_Lacre', NEW.NR_Lacre,
      'NR_Etiqueta', NEW.NR_Etiqueta,
      'NR_QRCode', NEW.NR_QRCode,
      'NR_CodigoBarra', NEW.NR_CodigoBarra,
      'NM_Titulo', NEW.NM_Titulo,
      'DS_Item', NEW.DS_Item,
      'DS_Observacoes', NEW.DS_Observacoes,
      'NM_Marca', NEW.NM_Marca,
      'NM_Modelo', NEW.NM_Modelo,
      'NM_Cor', NEW.NM_Cor,
      'NM_Estado', NEW.NM_Estado,
      'DS_Tags', NEW.DS_Tags,
      'NM_Material', NEW.NM_Material,
      'NM_Tamanho', NEW.NM_Tamanho,
      'NM_Serie', NEW.NM_Serie,
      'TP_Prioridade', NEW.TP_Prioridade,
      'NM_IMEI', NEW.NM_IMEI,
      'VL_Estimado', NEW.VL_Estimado,
      'DT_Encontrado', DATE_FORMAT(NEW.DT_Encontrado, '%Y-%m-%d'),
      'HR_Encontrado', TIME_FORMAT(NEW.HR_Encontrado, '%H:%i:%s'),
      'NM_LocalEncontrado', NEW.NM_LocalEncontrado,
      'NM_Posto', NEW.NM_Posto,
      'NM_EncontradoPor', NEW.NM_EncontradoPor,
      'FG_Lacrado', NEW.FG_Lacrado,
      'FG_Entregue', NEW.FG_Entregue,
      'FG_Descartado', NEW.FG_Descartado,
      'FG_Perigoso', NEW.FG_Perigoso,
      'FG_Sensivel', NEW.FG_Sensivel,
      'FG_Ativo', NEW.FG_Ativo,
      'FG_Excluido', NEW.FG_Excluido,
      'DT_Cadastro', DATE_FORMAT(NEW.DT_Cadastro, '%Y-%m-%d %H:%i:%s'),
      'DT_Alteracao', DATE_FORMAT(NEW.DT_Alteracao, '%Y-%m-%d %H:%i:%s')
    ), NEW.IDR_UsuarioCadastro);
  CALL SP_RegistrarVersionamento('item', NEW.ID_Item, 'INSERT',
    JSON_OBJECT('CD_Item', NEW.CD_Item, 'NM_Titulo', NEW.NM_Titulo, 'IDR_Evento', NEW.IDR_Evento,
                'IDR_Status', NEW.IDR_Status, 'IDR_Categoria', NEW.IDR_Categoria),
    NEW.IDR_UsuarioCadastro);
  IF NEW.IDR_Evento IS NOT NULL THEN
    CALL SP_IniciarSla(NEW.IDR_Evento, 'ITEM_ANALISE', 'ITEM', NEW.ID_Item, NEW.IDR_UsuarioCadastro);
  END IF;
END$$

CREATE TRIGGER TRG_item_au_audit AFTER UPDATE ON item FOR EACH ROW
BEGIN
  IF NOT (
      OLD.IDR_Evento <=> NEW.IDR_Evento AND OLD.IDR_Categoria <=> NEW.IDR_Categoria
      AND OLD.IDR_Subcategoria <=> NEW.IDR_Subcategoria AND OLD.IDR_Localizacao <=> NEW.IDR_Localizacao
      AND OLD.IDR_LocalAtual <=> NEW.IDR_LocalAtual AND OLD.IDR_Status <=> NEW.IDR_Status
      AND OLD.IDR_Lacre <=> NEW.IDR_Lacre AND OLD.CD_Item <=> NEW.CD_Item
      AND OLD.NR_Lacre <=> NEW.NR_Lacre AND OLD.NR_Etiqueta <=> NEW.NR_Etiqueta
      AND OLD.NR_QRCode <=> NEW.NR_QRCode AND OLD.NR_CodigoBarra <=> NEW.NR_CodigoBarra
      AND OLD.NM_Titulo <=> NEW.NM_Titulo AND OLD.DS_Item <=> NEW.DS_Item
      AND OLD.DS_Observacoes <=> NEW.DS_Observacoes AND OLD.NM_Marca <=> NEW.NM_Marca
      AND OLD.NM_Modelo <=> NEW.NM_Modelo AND OLD.NM_Cor <=> NEW.NM_Cor
      AND OLD.NM_Estado <=> NEW.NM_Estado AND OLD.DS_Tags <=> NEW.DS_Tags
      AND OLD.NM_Material <=> NEW.NM_Material AND OLD.NM_Tamanho <=> NEW.NM_Tamanho
      AND OLD.NM_Serie <=> NEW.NM_Serie AND OLD.TP_Prioridade <=> NEW.TP_Prioridade
      AND OLD.NM_IMEI <=> NEW.NM_IMEI AND OLD.VL_Estimado <=> NEW.VL_Estimado
      AND OLD.DT_Encontrado <=> NEW.DT_Encontrado AND OLD.HR_Encontrado <=> NEW.HR_Encontrado
      AND OLD.NM_LocalEncontrado <=> NEW.NM_LocalEncontrado AND OLD.NM_Posto <=> NEW.NM_Posto
      AND OLD.NM_EncontradoPor <=> NEW.NM_EncontradoPor AND OLD.FG_Lacrado <=> NEW.FG_Lacrado
      AND OLD.FG_Entregue <=> NEW.FG_Entregue AND OLD.FG_Descartado <=> NEW.FG_Descartado
      AND OLD.FG_Perigoso <=> NEW.FG_Perigoso AND OLD.FG_Sensivel <=> NEW.FG_Sensivel
      AND OLD.FG_Ativo <=> NEW.FG_Ativo AND OLD.FG_Excluido <=> NEW.FG_Excluido
  ) THEN
    CALL SP_RegistrarAuditoria('item', NEW.ID_Item, 'UPDATE',
      JSON_OBJECT(
        'IDR_Evento', OLD.IDR_Evento, 'IDR_Categoria', OLD.IDR_Categoria,
        'IDR_Subcategoria', OLD.IDR_Subcategoria, 'IDR_Localizacao', OLD.IDR_Localizacao,
        'IDR_LocalAtual', OLD.IDR_LocalAtual, 'IDR_Status', OLD.IDR_Status,
        'IDR_Lacre', OLD.IDR_Lacre, 'CD_Item', OLD.CD_Item, 'NR_Lacre', OLD.NR_Lacre,
        'NR_Etiqueta', OLD.NR_Etiqueta, 'NR_QRCode', OLD.NR_QRCode,
        'NR_CodigoBarra', OLD.NR_CodigoBarra, 'NM_Titulo', OLD.NM_Titulo,
        'DS_Item', OLD.DS_Item, 'DS_Observacoes', OLD.DS_Observacoes,
        'NM_Marca', OLD.NM_Marca, 'NM_Modelo', OLD.NM_Modelo, 'NM_Cor', OLD.NM_Cor,
        'NM_Estado', OLD.NM_Estado, 'DS_Tags', OLD.DS_Tags, 'NM_Material', OLD.NM_Material,
        'NM_Tamanho', OLD.NM_Tamanho, 'NM_Serie', OLD.NM_Serie, 'TP_Prioridade', OLD.TP_Prioridade,
        'NM_IMEI', OLD.NM_IMEI, 'VL_Estimado', OLD.VL_Estimado,
        'DT_Encontrado', DATE_FORMAT(OLD.DT_Encontrado, '%Y-%m-%d'),
        'HR_Encontrado', TIME_FORMAT(OLD.HR_Encontrado, '%H:%i:%s'),
        'NM_LocalEncontrado', OLD.NM_LocalEncontrado, 'NM_Posto', OLD.NM_Posto,
        'NM_EncontradoPor', OLD.NM_EncontradoPor, 'FG_Lacrado', OLD.FG_Lacrado,
        'FG_Entregue', OLD.FG_Entregue, 'FG_Descartado', OLD.FG_Descartado,
        'FG_Perigoso', OLD.FG_Perigoso, 'FG_Sensivel', OLD.FG_Sensivel,
        'FG_Ativo', OLD.FG_Ativo, 'FG_Excluido', OLD.FG_Excluido,
        'DT_Cadastro', DATE_FORMAT(OLD.DT_Cadastro, '%Y-%m-%d %H:%i:%s'),
        'DT_Alteracao', DATE_FORMAT(OLD.DT_Alteracao, '%Y-%m-%d %H:%i:%s')
      ),
      JSON_OBJECT(
        'IDR_Evento', NEW.IDR_Evento, 'IDR_Categoria', NEW.IDR_Categoria,
        'IDR_Subcategoria', NEW.IDR_Subcategoria, 'IDR_Localizacao', NEW.IDR_Localizacao,
        'IDR_LocalAtual', NEW.IDR_LocalAtual, 'IDR_Status', NEW.IDR_Status,
        'IDR_Lacre', NEW.IDR_Lacre, 'CD_Item', NEW.CD_Item, 'NR_Lacre', NEW.NR_Lacre,
        'NR_Etiqueta', NEW.NR_Etiqueta, 'NR_QRCode', NEW.NR_QRCode,
        'NR_CodigoBarra', NEW.NR_CodigoBarra, 'NM_Titulo', NEW.NM_Titulo,
        'DS_Item', NEW.DS_Item, 'DS_Observacoes', NEW.DS_Observacoes,
        'NM_Marca', NEW.NM_Marca, 'NM_Modelo', NEW.NM_Modelo, 'NM_Cor', NEW.NM_Cor,
        'NM_Estado', NEW.NM_Estado, 'DS_Tags', NEW.DS_Tags, 'NM_Material', NEW.NM_Material,
        'NM_Tamanho', NEW.NM_Tamanho, 'NM_Serie', NEW.NM_Serie, 'TP_Prioridade', NEW.TP_Prioridade,
        'NM_IMEI', NEW.NM_IMEI, 'VL_Estimado', NEW.VL_Estimado,
        'DT_Encontrado', DATE_FORMAT(NEW.DT_Encontrado, '%Y-%m-%d'),
        'HR_Encontrado', TIME_FORMAT(NEW.HR_Encontrado, '%H:%i:%s'),
        'NM_LocalEncontrado', NEW.NM_LocalEncontrado, 'NM_Posto', NEW.NM_Posto,
        'NM_EncontradoPor', NEW.NM_EncontradoPor, 'FG_Lacrado', NEW.FG_Lacrado,
        'FG_Entregue', NEW.FG_Entregue, 'FG_Descartado', NEW.FG_Descartado,
        'FG_Perigoso', NEW.FG_Perigoso, 'FG_Sensivel', NEW.FG_Sensivel,
        'FG_Ativo', NEW.FG_Ativo, 'FG_Excluido', NEW.FG_Excluido,
        'DT_Cadastro', DATE_FORMAT(NEW.DT_Cadastro, '%Y-%m-%d %H:%i:%s'),
        'DT_Alteracao', DATE_FORMAT(NEW.DT_Alteracao, '%Y-%m-%d %H:%i:%s')
      ),
      NEW.IDR_UsuarioAlteracao);
    CALL SP_RegistrarVersionamento('item', NEW.ID_Item, 'UPDATE',
      JSON_OBJECT('CD_Item', NEW.CD_Item, 'NM_Titulo', NEW.NM_Titulo, 'IDR_Status', NEW.IDR_Status,
                  'IDR_Localizacao', NEW.IDR_Localizacao, 'FG_Entregue', NEW.FG_Entregue,
                  'FG_Excluido', NEW.FG_Excluido, 'IDR_Lacre', NEW.IDR_Lacre),
      NEW.IDR_UsuarioAlteracao);
  END IF;
  IF NEW.FG_Entregue = 1 AND (OLD.FG_Entregue <=> 0 OR OLD.FG_Entregue IS NULL) THEN
    CALL SP_ConcluirSla('ITEM', NEW.ID_Item);
  END IF;
END$$

-- ---------------------------------------------------------------------
-- CLAIM
-- ---------------------------------------------------------------------
DROP TRIGGER IF EXISTS TRG_claim_ai_audit$$
DROP TRIGGER IF EXISTS TRG_claim_au_audit$$

CREATE TRIGGER TRG_claim_ai_audit AFTER INSERT ON claim FOR EACH ROW
BEGIN
  CALL SP_RegistrarAuditoria('claim', NEW.ID_Claim, 'INSERT', NULL,
    JSON_OBJECT(
      'IDR_Evento', NEW.IDR_Evento, 'IDR_Categoria', NEW.IDR_Categoria,
      'IDR_Subcategoria', NEW.IDR_Subcategoria, 'IDR_Status', NEW.IDR_Status,
      'TP_Claim', NEW.TP_Claim, 'CD_Claim', NEW.CD_Claim,
      'NM_Nome', NEW.NM_Nome, 'NR_CPF', NEW.NR_CPF, 'NM_Email', NEW.NM_Email,
      'NR_Telefone', NEW.NR_Telefone, 'NM_ContatoConfianca', NEW.NM_ContatoConfianca,
      'NR_TelefoneConfianca', NEW.NR_TelefoneConfianca,
      'DS_RelacaoContatoConfianca', NEW.DS_RelacaoContatoConfianca,
      'NM_Whatsapp', NEW.NM_Whatsapp, 'NM_Objeto', NEW.NM_Objeto,
      'DS_Objeto', NEW.DS_Objeto, 'DS_DetalhesOcultos', NEW.DS_DetalhesOcultos,
      'NM_Marca', NEW.NM_Marca, 'NM_Modelo', NEW.NM_Modelo, 'NM_Cor', NEW.NM_Cor,
      'NM_Estado', NEW.NM_Estado, 'DS_Tags', NEW.DS_Tags,
      'TP_Prioridade', NEW.TP_Prioridade, 'FG_Sensivel', NEW.FG_Sensivel,
      'DT_Perdeu', DATE_FORMAT(NEW.DT_Perdeu, '%Y-%m-%d'),
      'HR_Perdeu', TIME_FORMAT(NEW.HR_Perdeu, '%H:%i:%s'),
      'NM_Local', NEW.NM_Local, 'NM_Operador', NEW.NM_Operador,
      'IDR_Local', NEW.IDR_Local, 'DS_Observacao', NEW.DS_Observacao,
      'FG_Ativo', NEW.FG_Ativo, 'FG_Excluido', NEW.FG_Excluido,
      'DT_Cadastro', DATE_FORMAT(NEW.DT_Cadastro, '%Y-%m-%d %H:%i:%s'),
      'DT_Alteracao', DATE_FORMAT(NEW.DT_Alteracao, '%Y-%m-%d %H:%i:%s')
    ), NEW.IDR_UsuarioCadastro);
  CALL SP_RegistrarVersionamento('claim', NEW.ID_Claim, 'INSERT',
    JSON_OBJECT('NM_Nome', NEW.NM_Nome, 'NM_Objeto', NEW.NM_Objeto, 'IDR_Evento', NEW.IDR_Evento, 'IDR_Status', NEW.IDR_Status),
    NEW.IDR_UsuarioCadastro);
  CALL SP_IniciarSla(NEW.IDR_Evento, 'CLAIM_RESPOSTA', 'CLAIM', NEW.ID_Claim, NEW.IDR_UsuarioCadastro);
END$$

CREATE TRIGGER TRG_claim_au_audit AFTER UPDATE ON claim FOR EACH ROW
BEGIN
  IF NOT (
      OLD.IDR_Evento <=> NEW.IDR_Evento AND OLD.IDR_Categoria <=> NEW.IDR_Categoria
      AND OLD.IDR_Subcategoria <=> NEW.IDR_Subcategoria AND OLD.IDR_Status <=> NEW.IDR_Status
      AND OLD.TP_Claim <=> NEW.TP_Claim AND OLD.CD_Claim <=> NEW.CD_Claim
      AND OLD.NM_Nome <=> NEW.NM_Nome AND OLD.NR_CPF <=> NEW.NR_CPF
      AND OLD.NM_Email <=> NEW.NM_Email AND OLD.NR_Telefone <=> NEW.NR_Telefone
      AND OLD.NM_ContatoConfianca <=> NEW.NM_ContatoConfianca
      AND OLD.NR_TelefoneConfianca <=> NEW.NR_TelefoneConfianca
      AND OLD.DS_RelacaoContatoConfianca <=> NEW.DS_RelacaoContatoConfianca
      AND OLD.NM_Whatsapp <=> NEW.NM_Whatsapp AND OLD.NM_Objeto <=> NEW.NM_Objeto
      AND OLD.DS_Objeto <=> NEW.DS_Objeto AND OLD.DS_DetalhesOcultos <=> NEW.DS_DetalhesOcultos
      AND OLD.NM_Marca <=> NEW.NM_Marca AND OLD.NM_Modelo <=> NEW.NM_Modelo
      AND OLD.NM_Cor <=> NEW.NM_Cor AND OLD.NM_Estado <=> NEW.NM_Estado
      AND OLD.DS_Tags <=> NEW.DS_Tags
      AND OLD.DS_JustificativaAprovacao <=> NEW.DS_JustificativaAprovacao
      AND OLD.DS_JustificativaReprovacao <=> NEW.DS_JustificativaReprovacao
      AND OLD.TP_Prioridade <=> NEW.TP_Prioridade AND OLD.FG_Sensivel <=> NEW.FG_Sensivel
      AND OLD.DT_Perdeu <=> NEW.DT_Perdeu AND OLD.HR_Perdeu <=> NEW.HR_Perdeu
      AND OLD.NM_Local <=> NEW.NM_Local AND OLD.NM_Operador <=> NEW.NM_Operador
      AND OLD.IDR_Local <=> NEW.IDR_Local AND OLD.DS_Observacao <=> NEW.DS_Observacao
      AND OLD.FG_Ativo <=> NEW.FG_Ativo AND OLD.FG_Excluido <=> NEW.FG_Excluido
  ) THEN
    CALL SP_RegistrarAuditoria('claim', NEW.ID_Claim, 'UPDATE',
      JSON_OBJECT(
        'IDR_Evento', OLD.IDR_Evento, 'IDR_Categoria', OLD.IDR_Categoria,
        'IDR_Subcategoria', OLD.IDR_Subcategoria, 'IDR_Status', OLD.IDR_Status,
        'TP_Claim', OLD.TP_Claim, 'CD_Claim', OLD.CD_Claim,
        'NM_Nome', OLD.NM_Nome, 'NR_CPF', OLD.NR_CPF, 'NM_Email', OLD.NM_Email,
        'NR_Telefone', OLD.NR_Telefone, 'NM_ContatoConfianca', OLD.NM_ContatoConfianca,
        'NR_TelefoneConfianca', OLD.NR_TelefoneConfianca,
        'DS_RelacaoContatoConfianca', OLD.DS_RelacaoContatoConfianca,
        'NM_Whatsapp', OLD.NM_Whatsapp, 'NM_Objeto', OLD.NM_Objeto,
        'DS_Objeto', OLD.DS_Objeto, 'DS_DetalhesOcultos', OLD.DS_DetalhesOcultos,
        'NM_Marca', OLD.NM_Marca, 'NM_Modelo', OLD.NM_Modelo, 'NM_Cor', OLD.NM_Cor,
        'NM_Estado', OLD.NM_Estado, 'DS_Tags', OLD.DS_Tags,
        'DS_JustificativaAprovacao', OLD.DS_JustificativaAprovacao,
        'DS_JustificativaReprovacao', OLD.DS_JustificativaReprovacao,
        'TP_Prioridade', OLD.TP_Prioridade, 'FG_Sensivel', OLD.FG_Sensivel,
        'DT_Perdeu', DATE_FORMAT(OLD.DT_Perdeu, '%Y-%m-%d'),
        'HR_Perdeu', TIME_FORMAT(OLD.HR_Perdeu, '%H:%i:%s'),
        'NM_Local', OLD.NM_Local, 'NM_Operador', OLD.NM_Operador,
        'IDR_Local', OLD.IDR_Local, 'DS_Observacao', OLD.DS_Observacao,
        'FG_Ativo', OLD.FG_Ativo, 'FG_Excluido', OLD.FG_Excluido,
        'DT_Cadastro', DATE_FORMAT(OLD.DT_Cadastro, '%Y-%m-%d %H:%i:%s'),
        'DT_Alteracao', DATE_FORMAT(OLD.DT_Alteracao, '%Y-%m-%d %H:%i:%s')
      ),
      JSON_OBJECT(
        'IDR_Evento', NEW.IDR_Evento, 'IDR_Categoria', NEW.IDR_Categoria,
        'IDR_Subcategoria', NEW.IDR_Subcategoria, 'IDR_Status', NEW.IDR_Status,
        'TP_Claim', NEW.TP_Claim, 'CD_Claim', NEW.CD_Claim,
        'NM_Nome', NEW.NM_Nome, 'NR_CPF', NEW.NR_CPF, 'NM_Email', NEW.NM_Email,
        'NR_Telefone', NEW.NR_Telefone, 'NM_ContatoConfianca', NEW.NM_ContatoConfianca,
        'NR_TelefoneConfianca', NEW.NR_TelefoneConfianca,
        'DS_RelacaoContatoConfianca', NEW.DS_RelacaoContatoConfianca,
        'NM_Whatsapp', NEW.NM_Whatsapp, 'NM_Objeto', NEW.NM_Objeto,
        'DS_Objeto', NEW.DS_Objeto, 'DS_DetalhesOcultos', NEW.DS_DetalhesOcultos,
        'NM_Marca', NEW.NM_Marca, 'NM_Modelo', NEW.NM_Modelo, 'NM_Cor', NEW.NM_Cor,
        'NM_Estado', NEW.NM_Estado, 'DS_Tags', NEW.DS_Tags,
        'DS_JustificativaAprovacao', NEW.DS_JustificativaAprovacao,
        'DS_JustificativaReprovacao', NEW.DS_JustificativaReprovacao,
        'TP_Prioridade', NEW.TP_Prioridade, 'FG_Sensivel', NEW.FG_Sensivel,
        'DT_Perdeu', DATE_FORMAT(NEW.DT_Perdeu, '%Y-%m-%d'),
        'HR_Perdeu', TIME_FORMAT(NEW.HR_Perdeu, '%H:%i:%s'),
        'NM_Local', NEW.NM_Local, 'NM_Operador', NEW.NM_Operador,
        'IDR_Local', NEW.IDR_Local, 'DS_Observacao', NEW.DS_Observacao,
        'FG_Ativo', NEW.FG_Ativo, 'FG_Excluido', NEW.FG_Excluido,
        'DT_Cadastro', DATE_FORMAT(NEW.DT_Cadastro, '%Y-%m-%d %H:%i:%s'),
        'DT_Alteracao', DATE_FORMAT(NEW.DT_Alteracao, '%Y-%m-%d %H:%i:%s')
      ),
      NEW.IDR_UsuarioAlteracao);
    CALL SP_RegistrarVersionamento('claim', NEW.ID_Claim, 'UPDATE',
      JSON_OBJECT('NM_Nome', NEW.NM_Nome, 'NM_Objeto', NEW.NM_Objeto, 'IDR_Status', NEW.IDR_Status, 'FG_Excluido', NEW.FG_Excluido),
      NEW.IDR_UsuarioAlteracao);
  END IF;
END$$

-- ---------------------------------------------------------------------
-- TRIAGEM
-- ---------------------------------------------------------------------
DROP TRIGGER IF EXISTS TRG_triagem_ai_audit$$
DROP TRIGGER IF EXISTS TRG_triagem_au_audit$$

CREATE TRIGGER TRG_triagem_ai_audit AFTER INSERT ON triagem FOR EACH ROW
BEGIN
  CALL SP_RegistrarAuditoria('triagem', NEW.ID_Triagem, 'INSERT', NULL,
    JSON_OBJECT(
      'IDR_Item', NEW.IDR_Item, 'IDR_Operador', NEW.IDR_Operador,
      'IDR_LocalizacaoInicial', NEW.IDR_LocalizacaoInicial,
      'NM_Estado', NEW.NM_Estado, 'DS_Tags', NEW.DS_Tags,
      'DS_Observacao', NEW.DS_Observacao, 'DS_SugestaoIa', NEW.DS_SugestaoIa,
      'VL_ConfiancaIa', NEW.VL_ConfiancaIa, 'TP_Status', NEW.TP_Status,
      'DT_Inicio', DATE_FORMAT(NEW.DT_Inicio, '%Y-%m-%d %H:%i:%s'),
      'DT_Conclusao', DATE_FORMAT(NEW.DT_Conclusao, '%Y-%m-%d %H:%i:%s'),
      'FG_Ativo', NEW.FG_Ativo, 'FG_Excluido', NEW.FG_Excluido,
      'DT_Cadastro', DATE_FORMAT(NEW.DT_Cadastro, '%Y-%m-%d %H:%i:%s'),
      'DT_Alteracao', DATE_FORMAT(NEW.DT_Alteracao, '%Y-%m-%d %H:%i:%s')
    ), NEW.IDR_UsuarioCadastro);
END$$

CREATE TRIGGER TRG_triagem_au_audit AFTER UPDATE ON triagem FOR EACH ROW
BEGIN
  IF NOT (
      OLD.IDR_Item <=> NEW.IDR_Item AND OLD.IDR_Operador <=> NEW.IDR_Operador
      AND OLD.IDR_LocalizacaoInicial <=> NEW.IDR_LocalizacaoInicial
      AND OLD.NM_Estado <=> NEW.NM_Estado AND OLD.DS_Tags <=> NEW.DS_Tags
      AND OLD.DS_Observacao <=> NEW.DS_Observacao AND OLD.DS_SugestaoIa <=> NEW.DS_SugestaoIa
      AND OLD.VL_ConfiancaIa <=> NEW.VL_ConfiancaIa AND OLD.TP_Status <=> NEW.TP_Status
      AND OLD.DT_Inicio <=> NEW.DT_Inicio AND OLD.DT_Conclusao <=> NEW.DT_Conclusao
      AND OLD.FG_Ativo <=> NEW.FG_Ativo AND OLD.FG_Excluido <=> NEW.FG_Excluido
  ) THEN
    CALL SP_RegistrarAuditoria('triagem', NEW.ID_Triagem, 'UPDATE',
      JSON_OBJECT(
        'IDR_Item', OLD.IDR_Item, 'IDR_Operador', OLD.IDR_Operador,
        'IDR_LocalizacaoInicial', OLD.IDR_LocalizacaoInicial,
        'NM_Estado', OLD.NM_Estado, 'DS_Tags', OLD.DS_Tags,
        'DS_Observacao', OLD.DS_Observacao, 'DS_SugestaoIa', OLD.DS_SugestaoIa,
        'VL_ConfiancaIa', OLD.VL_ConfiancaIa, 'TP_Status', OLD.TP_Status,
        'DT_Inicio', DATE_FORMAT(OLD.DT_Inicio, '%Y-%m-%d %H:%i:%s'),
        'DT_Conclusao', DATE_FORMAT(OLD.DT_Conclusao, '%Y-%m-%d %H:%i:%s'),
        'FG_Ativo', OLD.FG_Ativo, 'FG_Excluido', OLD.FG_Excluido,
        'DT_Cadastro', DATE_FORMAT(OLD.DT_Cadastro, '%Y-%m-%d %H:%i:%s'),
        'DT_Alteracao', DATE_FORMAT(OLD.DT_Alteracao, '%Y-%m-%d %H:%i:%s')
      ),
      JSON_OBJECT(
        'IDR_Item', NEW.IDR_Item, 'IDR_Operador', NEW.IDR_Operador,
        'IDR_LocalizacaoInicial', NEW.IDR_LocalizacaoInicial,
        'NM_Estado', NEW.NM_Estado, 'DS_Tags', NEW.DS_Tags,
        'DS_Observacao', NEW.DS_Observacao, 'DS_SugestaoIa', NEW.DS_SugestaoIa,
        'VL_ConfiancaIa', NEW.VL_ConfiancaIa, 'TP_Status', NEW.TP_Status,
        'DT_Inicio', DATE_FORMAT(NEW.DT_Inicio, '%Y-%m-%d %H:%i:%s'),
        'DT_Conclusao', DATE_FORMAT(NEW.DT_Conclusao, '%Y-%m-%d %H:%i:%s'),
        'FG_Ativo', NEW.FG_Ativo, 'FG_Excluido', NEW.FG_Excluido,
        'DT_Cadastro', DATE_FORMAT(NEW.DT_Cadastro, '%Y-%m-%d %H:%i:%s'),
        'DT_Alteracao', DATE_FORMAT(NEW.DT_Alteracao, '%Y-%m-%d %H:%i:%s')
      ),
      NEW.IDR_UsuarioAlteracao);
  END IF;
END$$

-- ---------------------------------------------------------------------
-- DEVOLUCAO
-- ---------------------------------------------------------------------
DROP TRIGGER IF EXISTS TRG_devolucao_ai_audit$$
DROP TRIGGER IF EXISTS TRG_devolucao_au_audit$$

CREATE TRIGGER TRG_devolucao_ai_audit AFTER INSERT ON devolucao FOR EACH ROW
BEGIN
  CALL SP_RegistrarAuditoria('devolucao', NEW.ID_Devolucao, 'INSERT', NULL,
    JSON_OBJECT(
      'IDR_Evento', NEW.IDR_Evento, 'IDR_Item', NEW.IDR_Item, 'IDR_Claim', NEW.IDR_Claim,
      'TP_Devolucao', NEW.TP_Devolucao,
      'DT_Devolucao', DATE_FORMAT(NEW.DT_Devolucao, '%Y-%m-%d %H:%i:%s'),
      'NM_Recebedor', NEW.NM_Recebedor, 'NR_CPF', NEW.NR_CPF,
      'DS_Observacao', NEW.DS_Observacao, 'FG_Assinado', NEW.FG_Assinado,
      'FG_Concluido', NEW.FG_Concluido, 'TP_Status', NEW.TP_Status,
      'FG_Ativo', NEW.FG_Ativo, 'FG_Excluido', NEW.FG_Excluido,
      'DT_Cadastro', DATE_FORMAT(NEW.DT_Cadastro, '%Y-%m-%d %H:%i:%s'),
      'DT_Alteracao', DATE_FORMAT(NEW.DT_Alteracao, '%Y-%m-%d %H:%i:%s')
    ), NEW.IDR_UsuarioCadastro);
  CALL SP_RegistrarVersionamento('devolucao', NEW.ID_Devolucao, 'INSERT',
    JSON_OBJECT('IDR_Item', NEW.IDR_Item, 'IDR_Claim', NEW.IDR_Claim, 'TP_Devolucao', NEW.TP_Devolucao, 'NM_Recebedor', NEW.NM_Recebedor),
    NEW.IDR_UsuarioCadastro);
  CALL SP_IniciarSla(NULL, 'DEVOLUCAO', 'DEVOLUCAO', NEW.ID_Devolucao, NEW.IDR_UsuarioCadastro);
END$$

CREATE TRIGGER TRG_devolucao_au_audit AFTER UPDATE ON devolucao FOR EACH ROW
BEGIN
  IF NOT (
      OLD.IDR_Evento <=> NEW.IDR_Evento AND OLD.IDR_Item <=> NEW.IDR_Item
      AND OLD.IDR_Claim <=> NEW.IDR_Claim AND OLD.TP_Devolucao <=> NEW.TP_Devolucao
      AND OLD.DT_Devolucao <=> NEW.DT_Devolucao AND OLD.NM_Recebedor <=> NEW.NM_Recebedor
      AND OLD.NR_CPF <=> NEW.NR_CPF AND OLD.DS_Observacao <=> NEW.DS_Observacao
      AND OLD.FG_Assinado <=> NEW.FG_Assinado AND OLD.FG_Concluido <=> NEW.FG_Concluido
      AND OLD.TP_Status <=> NEW.TP_Status AND OLD.FG_Ativo <=> NEW.FG_Ativo
      AND OLD.FG_Excluido <=> NEW.FG_Excluido
  ) THEN
    CALL SP_RegistrarAuditoria('devolucao', NEW.ID_Devolucao, 'UPDATE',
      JSON_OBJECT(
        'IDR_Evento', OLD.IDR_Evento, 'IDR_Item', OLD.IDR_Item, 'IDR_Claim', OLD.IDR_Claim,
        'TP_Devolucao', OLD.TP_Devolucao,
        'DT_Devolucao', DATE_FORMAT(OLD.DT_Devolucao, '%Y-%m-%d %H:%i:%s'),
        'NM_Recebedor', OLD.NM_Recebedor, 'NR_CPF', OLD.NR_CPF,
        'DS_Observacao', OLD.DS_Observacao, 'FG_Assinado', OLD.FG_Assinado,
        'FG_Concluido', OLD.FG_Concluido, 'TP_Status', OLD.TP_Status,
        'FG_Ativo', OLD.FG_Ativo, 'FG_Excluido', OLD.FG_Excluido,
        'DT_Cadastro', DATE_FORMAT(OLD.DT_Cadastro, '%Y-%m-%d %H:%i:%s'),
        'DT_Alteracao', DATE_FORMAT(OLD.DT_Alteracao, '%Y-%m-%d %H:%i:%s')
      ),
      JSON_OBJECT(
        'IDR_Evento', NEW.IDR_Evento, 'IDR_Item', NEW.IDR_Item, 'IDR_Claim', NEW.IDR_Claim,
        'TP_Devolucao', NEW.TP_Devolucao,
        'DT_Devolucao', DATE_FORMAT(NEW.DT_Devolucao, '%Y-%m-%d %H:%i:%s'),
        'NM_Recebedor', NEW.NM_Recebedor, 'NR_CPF', NEW.NR_CPF,
        'DS_Observacao', NEW.DS_Observacao, 'FG_Assinado', NEW.FG_Assinado,
        'FG_Concluido', NEW.FG_Concluido, 'TP_Status', NEW.TP_Status,
        'FG_Ativo', NEW.FG_Ativo, 'FG_Excluido', NEW.FG_Excluido,
        'DT_Cadastro', DATE_FORMAT(NEW.DT_Cadastro, '%Y-%m-%d %H:%i:%s'),
        'DT_Alteracao', DATE_FORMAT(NEW.DT_Alteracao, '%Y-%m-%d %H:%i:%s')
      ),
      NEW.IDR_UsuarioAlteracao);
    CALL SP_RegistrarVersionamento('devolucao', NEW.ID_Devolucao, 'UPDATE',
      JSON_OBJECT('FG_Concluido', NEW.FG_Concluido, 'FG_Assinado', NEW.FG_Assinado, 'FG_Excluido', NEW.FG_Excluido),
      NEW.IDR_UsuarioAlteracao);
  END IF;
  IF NEW.FG_Concluido = 1 AND (OLD.FG_Concluido <=> 0 OR OLD.FG_Concluido IS NULL) THEN
    CALL SP_ConcluirSla('DEVOLUCAO', NEW.ID_Devolucao);
  END IF;
END$$

-- ---------------------------------------------------------------------
-- ARQUIVO / EVENTO / TRANSFERENCIA
-- ---------------------------------------------------------------------
DROP TRIGGER IF EXISTS TRG_arquivo_ai_audit$$
DROP TRIGGER IF EXISTS TRG_arquivo_au_audit$$

CREATE TRIGGER TRG_arquivo_ai_audit AFTER INSERT ON arquivo FOR EACH ROW
BEGIN
  CALL SP_RegistrarAuditoria('arquivo', NEW.ID_Arquivo, 'INSERT', NULL,
    JSON_OBJECT(
      'IDR_Evento', NEW.IDR_Evento, 'TP_Entidade', NEW.TP_Entidade, 'ID_Entidade', NEW.ID_Entidade,
      'TP_Arquivo', NEW.TP_Arquivo, 'NM_Arquivo', NEW.NM_Arquivo, 'NM_Path', NEW.NM_Path,
      'TP_Storage', NEW.TP_Storage, 'TP_Mime', NEW.TP_Mime, 'FG_Principal', NEW.FG_Principal,
      'NR_Largura', NEW.NR_Largura, 'NR_Altura', NEW.NR_Altura, 'QT_Bytes', NEW.QT_Bytes,
      'QT_Duracao', NEW.QT_Duracao, 'FG_Ativo', NEW.FG_Ativo, 'FG_Excluido', NEW.FG_Excluido,
      'DT_Cadastro', DATE_FORMAT(NEW.DT_Cadastro, '%Y-%m-%d %H:%i:%s'),
      'DT_Alteracao', DATE_FORMAT(NEW.DT_Alteracao, '%Y-%m-%d %H:%i:%s')
    ), NEW.IDR_UsuarioCadastro);
END$$

CREATE TRIGGER TRG_arquivo_au_audit AFTER UPDATE ON arquivo FOR EACH ROW
BEGIN
  IF NOT (
      OLD.TP_Entidade <=> NEW.TP_Entidade AND OLD.ID_Entidade <=> NEW.ID_Entidade
      AND OLD.TP_Arquivo <=> NEW.TP_Arquivo AND OLD.NM_Arquivo <=> NEW.NM_Arquivo
      AND OLD.NM_Path <=> NEW.NM_Path AND OLD.TP_Storage <=> NEW.TP_Storage
      AND OLD.TP_Mime <=> NEW.TP_Mime AND OLD.FG_Principal <=> NEW.FG_Principal
      AND OLD.FG_Ativo <=> NEW.FG_Ativo AND OLD.FG_Excluido <=> NEW.FG_Excluido
  ) THEN
    CALL SP_RegistrarAuditoria('arquivo', NEW.ID_Arquivo, 'UPDATE',
      JSON_OBJECT(
        'IDR_Evento', OLD.IDR_Evento, 'TP_Entidade', OLD.TP_Entidade, 'ID_Entidade', OLD.ID_Entidade,
        'TP_Arquivo', OLD.TP_Arquivo, 'NM_Arquivo', OLD.NM_Arquivo, 'NM_Path', OLD.NM_Path,
        'TP_Storage', OLD.TP_Storage, 'TP_Mime', OLD.TP_Mime, 'FG_Principal', OLD.FG_Principal,
        'NR_Largura', OLD.NR_Largura, 'NR_Altura', OLD.NR_Altura, 'QT_Bytes', OLD.QT_Bytes,
        'FG_Ativo', OLD.FG_Ativo, 'FG_Excluido', OLD.FG_Excluido,
        'DT_Cadastro', DATE_FORMAT(OLD.DT_Cadastro, '%Y-%m-%d %H:%i:%s'),
        'DT_Alteracao', DATE_FORMAT(OLD.DT_Alteracao, '%Y-%m-%d %H:%i:%s')
      ),
      JSON_OBJECT(
        'IDR_Evento', NEW.IDR_Evento, 'TP_Entidade', NEW.TP_Entidade, 'ID_Entidade', NEW.ID_Entidade,
        'TP_Arquivo', NEW.TP_Arquivo, 'NM_Arquivo', NEW.NM_Arquivo, 'NM_Path', NEW.NM_Path,
        'TP_Storage', NEW.TP_Storage, 'TP_Mime', NEW.TP_Mime, 'FG_Principal', NEW.FG_Principal,
        'NR_Largura', NEW.NR_Largura, 'NR_Altura', NEW.NR_Altura, 'QT_Bytes', NEW.QT_Bytes,
        'FG_Ativo', NEW.FG_Ativo, 'FG_Excluido', NEW.FG_Excluido,
        'DT_Cadastro', DATE_FORMAT(NEW.DT_Cadastro, '%Y-%m-%d %H:%i:%s'),
        'DT_Alteracao', DATE_FORMAT(NEW.DT_Alteracao, '%Y-%m-%d %H:%i:%s')
      ),
      NEW.IDR_UsuarioAlteracao);
  END IF;
END$$

DROP TRIGGER IF EXISTS TRG_evento_ai_audit$$
DROP TRIGGER IF EXISTS TRG_evento_au_audit$$

CREATE TRIGGER TRG_evento_ai_audit AFTER INSERT ON evento FOR EACH ROW
BEGIN
  CALL SP_RegistrarAuditoria('evento', NEW.ID_Evento, 'INSERT', NULL,
    JSON_OBJECT(
      'IDR_Empresa', NEW.IDR_Empresa, 'NM_Evento', NEW.NM_Evento, 'DS_Evento', NEW.DS_Evento,
      'DT_Inicio', DATE_FORMAT(NEW.DT_Inicio, '%Y-%m-%d %H:%i:%s'),
      'DT_Fim', DATE_FORMAT(NEW.DT_Fim, '%Y-%m-%d %H:%i:%s'),
      'DT_InicioRecebimento', DATE_FORMAT(NEW.DT_InicioRecebimento, '%Y-%m-%d %H:%i:%s'),
      'DT_FimRecebimento', DATE_FORMAT(NEW.DT_FimRecebimento, '%Y-%m-%d %H:%i:%s'),
      'DT_InicioConsulta', DATE_FORMAT(NEW.DT_InicioConsulta, '%Y-%m-%d %H:%i:%s'),
      'DT_FimConsulta', DATE_FORMAT(NEW.DT_FimConsulta, '%Y-%m-%d %H:%i:%s'),
      'DT_LimiteRetirada', DATE_FORMAT(NEW.DT_LimiteRetirada, '%Y-%m-%d %H:%i:%s'),
      'NM_Local', NEW.NM_Local, 'NM_Cidade', NEW.NM_Cidade, 'SG_UF', NEW.SG_UF,
      'QT_DiasRetencao', NEW.QT_DiasRetencao, 'FG_Ativo', NEW.FG_Ativo, 'FG_Excluido', NEW.FG_Excluido,
      'DT_Cadastro', DATE_FORMAT(NEW.DT_Cadastro, '%Y-%m-%d %H:%i:%s'),
      'DT_Alteracao', DATE_FORMAT(NEW.DT_Alteracao, '%Y-%m-%d %H:%i:%s')
    ), NEW.IDR_UsuarioCadastro);
  CALL SP_RegistrarVersionamento('evento', NEW.ID_Evento, 'INSERT',
    JSON_OBJECT('NM_Evento', NEW.NM_Evento, 'IDR_Empresa', NEW.IDR_Empresa,
                'DT_Inicio', DATE_FORMAT(NEW.DT_Inicio, '%Y-%m-%d %H:%i:%s'),
                'DT_Fim', DATE_FORMAT(NEW.DT_Fim, '%Y-%m-%d %H:%i:%s')),
    NEW.IDR_UsuarioCadastro);
END$$

CREATE TRIGGER TRG_evento_au_audit AFTER UPDATE ON evento FOR EACH ROW
BEGIN
  IF NOT (
      OLD.IDR_Empresa <=> NEW.IDR_Empresa AND OLD.NM_Evento <=> NEW.NM_Evento
      AND OLD.DS_Evento <=> NEW.DS_Evento AND OLD.DT_Inicio <=> NEW.DT_Inicio
      AND OLD.DT_Fim <=> NEW.DT_Fim AND OLD.DT_InicioRecebimento <=> NEW.DT_InicioRecebimento
      AND OLD.DT_FimRecebimento <=> NEW.DT_FimRecebimento
      AND OLD.DT_InicioConsulta <=> NEW.DT_InicioConsulta AND OLD.DT_FimConsulta <=> NEW.DT_FimConsulta
      AND OLD.DT_LimiteRetirada <=> NEW.DT_LimiteRetirada AND OLD.NM_Local <=> NEW.NM_Local
      AND OLD.NM_Cidade <=> NEW.NM_Cidade AND OLD.SG_UF <=> NEW.SG_UF
      AND OLD.QT_DiasRetencao <=> NEW.QT_DiasRetencao
      AND OLD.FG_Ativo <=> NEW.FG_Ativo AND OLD.FG_Excluido <=> NEW.FG_Excluido
  ) THEN
    CALL SP_RegistrarAuditoria('evento', NEW.ID_Evento, 'UPDATE',
      JSON_OBJECT(
        'IDR_Empresa', OLD.IDR_Empresa, 'NM_Evento', OLD.NM_Evento, 'DS_Evento', OLD.DS_Evento,
        'DT_Inicio', DATE_FORMAT(OLD.DT_Inicio, '%Y-%m-%d %H:%i:%s'),
        'DT_Fim', DATE_FORMAT(OLD.DT_Fim, '%Y-%m-%d %H:%i:%s'),
        'DT_InicioRecebimento', DATE_FORMAT(OLD.DT_InicioRecebimento, '%Y-%m-%d %H:%i:%s'),
        'DT_FimRecebimento', DATE_FORMAT(OLD.DT_FimRecebimento, '%Y-%m-%d %H:%i:%s'),
        'DT_InicioConsulta', DATE_FORMAT(OLD.DT_InicioConsulta, '%Y-%m-%d %H:%i:%s'),
        'DT_FimConsulta', DATE_FORMAT(OLD.DT_FimConsulta, '%Y-%m-%d %H:%i:%s'),
        'DT_LimiteRetirada', DATE_FORMAT(OLD.DT_LimiteRetirada, '%Y-%m-%d %H:%i:%s'),
        'NM_Local', OLD.NM_Local, 'NM_Cidade', OLD.NM_Cidade, 'SG_UF', OLD.SG_UF,
        'QT_DiasRetencao', OLD.QT_DiasRetencao, 'FG_Ativo', OLD.FG_Ativo, 'FG_Excluido', OLD.FG_Excluido,
        'DT_Cadastro', DATE_FORMAT(OLD.DT_Cadastro, '%Y-%m-%d %H:%i:%s'),
        'DT_Alteracao', DATE_FORMAT(OLD.DT_Alteracao, '%Y-%m-%d %H:%i:%s')
      ),
      JSON_OBJECT(
        'IDR_Empresa', NEW.IDR_Empresa, 'NM_Evento', NEW.NM_Evento, 'DS_Evento', NEW.DS_Evento,
        'DT_Inicio', DATE_FORMAT(NEW.DT_Inicio, '%Y-%m-%d %H:%i:%s'),
        'DT_Fim', DATE_FORMAT(NEW.DT_Fim, '%Y-%m-%d %H:%i:%s'),
        'DT_InicioRecebimento', DATE_FORMAT(NEW.DT_InicioRecebimento, '%Y-%m-%d %H:%i:%s'),
        'DT_FimRecebimento', DATE_FORMAT(NEW.DT_FimRecebimento, '%Y-%m-%d %H:%i:%s'),
        'DT_InicioConsulta', DATE_FORMAT(NEW.DT_InicioConsulta, '%Y-%m-%d %H:%i:%s'),
        'DT_FimConsulta', DATE_FORMAT(NEW.DT_FimConsulta, '%Y-%m-%d %H:%i:%s'),
        'DT_LimiteRetirada', DATE_FORMAT(NEW.DT_LimiteRetirada, '%Y-%m-%d %H:%i:%s'),
        'NM_Local', NEW.NM_Local, 'NM_Cidade', NEW.NM_Cidade, 'SG_UF', NEW.SG_UF,
        'QT_DiasRetencao', NEW.QT_DiasRetencao, 'FG_Ativo', NEW.FG_Ativo, 'FG_Excluido', NEW.FG_Excluido,
        'DT_Cadastro', DATE_FORMAT(NEW.DT_Cadastro, '%Y-%m-%d %H:%i:%s'),
        'DT_Alteracao', DATE_FORMAT(NEW.DT_Alteracao, '%Y-%m-%d %H:%i:%s')
      ),
      NEW.IDR_UsuarioAlteracao);
    CALL SP_RegistrarVersionamento('evento', NEW.ID_Evento, 'UPDATE',
      JSON_OBJECT('NM_Evento', NEW.NM_Evento, 'FG_Ativo', NEW.FG_Ativo, 'FG_Excluido', NEW.FG_Excluido),
      NEW.IDR_UsuarioAlteracao);
  END IF;
END$$

DROP TRIGGER IF EXISTS TRG_transferencia_ai_audit$$
DROP TRIGGER IF EXISTS TRG_transferencia_au_audit$$

CREATE TRIGGER TRG_transferencia_ai_audit AFTER INSERT ON transferencia FOR EACH ROW
BEGIN
  CALL SP_RegistrarAuditoria('transferencia', NEW.ID_Transferencia, 'INSERT', NULL,
    JSON_OBJECT(
      'IDR_Evento', NEW.IDR_Evento, 'IDR_Item', NEW.IDR_Item,
      'IDR_LocalOrigem', NEW.IDR_LocalOrigem, 'IDR_LocalDestino', NEW.IDR_LocalDestino,
      'IDR_UsuarioResponsavel', NEW.IDR_UsuarioResponsavel, 'NM_Receptor', NEW.NM_Receptor,
      'DS_Motivo', NEW.DS_Motivo, 'TP_Status', NEW.TP_Status,
      'DT_Transferencia', DATE_FORMAT(NEW.DT_Transferencia, '%Y-%m-%d %H:%i:%s'),
      'FG_Ativo', NEW.FG_Ativo, 'FG_Excluido', NEW.FG_Excluido,
      'DT_Cadastro', DATE_FORMAT(NEW.DT_Cadastro, '%Y-%m-%d %H:%i:%s'),
      'DT_Alteracao', DATE_FORMAT(NEW.DT_Alteracao, '%Y-%m-%d %H:%i:%s')
    ), NEW.IDR_UsuarioCadastro);
END$$

CREATE TRIGGER TRG_transferencia_au_audit AFTER UPDATE ON transferencia FOR EACH ROW
BEGIN
  IF NOT (
      OLD.IDR_Item <=> NEW.IDR_Item AND OLD.IDR_LocalOrigem <=> NEW.IDR_LocalOrigem
      AND OLD.IDR_LocalDestino <=> NEW.IDR_LocalDestino
      AND OLD.IDR_UsuarioResponsavel <=> NEW.IDR_UsuarioResponsavel
      AND OLD.NM_Receptor <=> NEW.NM_Receptor AND OLD.DS_Motivo <=> NEW.DS_Motivo
      AND OLD.TP_Status <=> NEW.TP_Status AND OLD.DT_Transferencia <=> NEW.DT_Transferencia
      AND OLD.FG_Ativo <=> NEW.FG_Ativo AND OLD.FG_Excluido <=> NEW.FG_Excluido
  ) THEN
    CALL SP_RegistrarAuditoria('transferencia', NEW.ID_Transferencia, 'UPDATE',
      JSON_OBJECT(
        'IDR_Evento', OLD.IDR_Evento, 'IDR_Item', OLD.IDR_Item,
        'IDR_LocalOrigem', OLD.IDR_LocalOrigem, 'IDR_LocalDestino', OLD.IDR_LocalDestino,
        'IDR_UsuarioResponsavel', OLD.IDR_UsuarioResponsavel, 'NM_Receptor', OLD.NM_Receptor,
        'DS_Motivo', OLD.DS_Motivo, 'TP_Status', OLD.TP_Status,
        'DT_Transferencia', DATE_FORMAT(OLD.DT_Transferencia, '%Y-%m-%d %H:%i:%s'),
        'FG_Ativo', OLD.FG_Ativo, 'FG_Excluido', OLD.FG_Excluido,
        'DT_Cadastro', DATE_FORMAT(OLD.DT_Cadastro, '%Y-%m-%d %H:%i:%s'),
        'DT_Alteracao', DATE_FORMAT(OLD.DT_Alteracao, '%Y-%m-%d %H:%i:%s')
      ),
      JSON_OBJECT(
        'IDR_Evento', NEW.IDR_Evento, 'IDR_Item', NEW.IDR_Item,
        'IDR_LocalOrigem', NEW.IDR_LocalOrigem, 'IDR_LocalDestino', NEW.IDR_LocalDestino,
        'IDR_UsuarioResponsavel', NEW.IDR_UsuarioResponsavel, 'NM_Receptor', NEW.NM_Receptor,
        'DS_Motivo', NEW.DS_Motivo, 'TP_Status', NEW.TP_Status,
        'DT_Transferencia', DATE_FORMAT(NEW.DT_Transferencia, '%Y-%m-%d %H:%i:%s'),
        'FG_Ativo', NEW.FG_Ativo, 'FG_Excluido', NEW.FG_Excluido,
        'DT_Cadastro', DATE_FORMAT(NEW.DT_Cadastro, '%Y-%m-%d %H:%i:%s'),
        'DT_Alteracao', DATE_FORMAT(NEW.DT_Alteracao, '%Y-%m-%d %H:%i:%s')
      ),
      NEW.IDR_UsuarioAlteracao);
  END IF;
END$$

DELIMITER ;
