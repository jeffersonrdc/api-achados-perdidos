-- =====================================================================
-- 057_Claim_Wallpaper.sql
-- Descrição do wallpaper do celular no relato de perda (portal /registrar).
-- Executar uma única vez.
-- =====================================================================

ALTER TABLE claim
  ADD COLUMN DS_Wallpaper VARCHAR(300) NULL
    COMMENT 'Descrição do papel de parede / tela de bloqueio (celulares)'
    AFTER DS_Objeto;

-- Atualiza triggers de auditoria do claim para incluir o novo campo.
DROP TRIGGER IF EXISTS TRG_claim_ai_audit;
DROP TRIGGER IF EXISTS TRG_claim_au_audit;

DELIMITER $$

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
      'DS_Objeto', NEW.DS_Objeto, 'DS_Wallpaper', NEW.DS_Wallpaper,
      'DS_DetalhesOcultos', NEW.DS_DetalhesOcultos,
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
      AND OLD.DS_Objeto <=> NEW.DS_Objeto AND OLD.DS_Wallpaper <=> NEW.DS_Wallpaper
      AND OLD.DS_DetalhesOcultos <=> NEW.DS_DetalhesOcultos
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
        'DS_Objeto', OLD.DS_Objeto, 'DS_Wallpaper', OLD.DS_Wallpaper,
        'DS_DetalhesOcultos', OLD.DS_DetalhesOcultos,
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
        'DS_Objeto', NEW.DS_Objeto, 'DS_Wallpaper', NEW.DS_Wallpaper,
        'DS_DetalhesOcultos', NEW.DS_DetalhesOcultos,
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

DELIMITER ;
