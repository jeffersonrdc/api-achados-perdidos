-- =====================================================================
-- 022_Permissionamento.sql
-- Permissionamento por modulo + acao. Perfil agrupa permissoes; usuario
-- herda as do perfil e pode ter permissoes ADICIONAIS (usuario_permissao).
-- Executar uma unica vez.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) permissao: colunas de modulo/acao + expansao do catalogo
-- ---------------------------------------------------------------------
ALTER TABLE permissao
  ADD COLUMN NM_Modulo VARCHAR(50) NULL AFTER NM_Permissao,
  ADD COLUMN NM_Acao   VARCHAR(50) NULL AFTER NM_Modulo;

INSERT IGNORE INTO permissao (NM_Permissao, DS_Permissao) VALUES
 ('dashboard.visualizar','Visualizar dashboard'),
 ('evento.listar','Listar eventos'), ('evento.criar','Criar eventos'), ('evento.editar','Editar eventos'), ('evento.excluir','Excluir eventos'),
 ('configuracao.gerenciar','Gerenciar configuracao do evento'),
 ('item.listar','Listar itens'), ('item.criar','Cadastrar itens'), ('item.editar','Editar itens'), ('item.excluir','Excluir itens'),
 ('item.movimentar','Movimentar itens'), ('item.transicionar','Alterar status do item'), ('item.campos','Gerenciar campos dinamicos do item'),
 ('triagem.listar','Listar fila de triagem'), ('triagem.iniciar','Iniciar triagem'), ('triagem.salvar','Salvar triagem'), ('triagem.concluir','Concluir triagem'),
 ('etiqueta.visualizar','Visualizar etiqueta'), ('etiqueta.imprimir','Imprimir/reimprimir etiqueta'),
 ('deposito.listar','Listar depositos'), ('deposito.gerenciar','Gerenciar depositos'),
 ('localizacao.listar','Listar localizacoes'), ('localizacao.gerenciar','Gerenciar localizacoes'),
 ('categoria.listar','Listar categorias'), ('categoria.gerenciar','Gerenciar categorias'), ('categoria.campos','Gerenciar campos de categoria'),
 ('status.listar','Listar status de item'),
 ('claim.listar','Listar claims'), ('claim.criar','Criar claims'), ('claim.editar','Editar claims'), ('claim.excluir','Excluir claims'), ('claim.validar','Validar claims'),
 ('devolucao.listar','Listar devolucoes'), ('devolucao.realizar','Realizar devolucoes'),
 ('local.listar','Listar locais'), ('local.criar','Criar locais'), ('local.editar','Editar locais'), ('local.excluir','Excluir locais'),
 ('equipe.listar','Listar equipes'), ('equipe.criar','Criar equipes'), ('equipe.editar','Editar equipes'), ('equipe.excluir','Excluir equipes'), ('equipe.membros','Gerenciar membros de equipe'),
 ('usuario.listar','Listar usuarios'), ('usuario.criar','Criar usuarios'), ('usuario.editar','Editar usuarios'), ('usuario.excluir','Excluir usuarios'), ('usuario.permissoes','Gerenciar permissoes de usuario'),
 ('relatorio.visualizar','Visualizar relatorios'),
 ('analytics.visualizar','Visualizar analytics'),
 ('auditoria.consultar','Consultar auditoria'),
 ('lacre.listar','Listar lacres'), ('lacre.gerenciar','Gerenciar lacres'),
 ('contato.listar','Listar contatos'), ('contato.gerenciar','Gerenciar contatos'),
 ('crianca.listar','Listar criancas'), ('crianca.gerenciar','Gerenciar criancas'),
 ('arquivo.listar','Listar arquivos'), ('arquivo.gerenciar','Gerenciar arquivos'),
 ('sla.listar','Listar SLA'), ('sla.gerenciar','Gerenciar SLA'),
 ('perfil.listar','Listar perfis'), ('perfil.gerenciar','Gerenciar perfis e suas permissoes'),
 ('permissao.listar','Listar catalogo de permissoes');

UPDATE permissao
  SET NM_Modulo = SUBSTRING_INDEX(NM_Permissao, '.', 1),
      NM_Acao   = SUBSTRING_INDEX(NM_Permissao, '.', -1);

-- ---------------------------------------------------------------------
-- 2) usuario_permissao (permissoes adicionais por usuario)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS usuario_permissao (
  ID_UsuarioPermissao  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  IDR_Usuario          BIGINT UNSIGNED NOT NULL,
  IDR_Permissao        BIGINT UNSIGNED NOT NULL,
  DT_Cadastro          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  DT_Alteracao         DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
  IDR_UsuarioCadastro  BIGINT UNSIGNED NULL,
  IDR_UsuarioAlteracao BIGINT UNSIGNED NULL,
  FG_Ativo             TINYINT(1) NOT NULL DEFAULT 1,
  FG_Excluido          TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (ID_UsuarioPermissao),
  UNIQUE KEY UQ_usuario_permissao (IDR_Usuario, IDR_Permissao),
  CONSTRAINT FK_usuperm_usuario   FOREIGN KEY (IDR_Usuario)          REFERENCES usuario   (ID_Usuario),
  CONSTRAINT FK_usuperm_permissao FOREIGN KEY (IDR_Permissao)        REFERENCES permissao (ID_Permissao),
  CONSTRAINT FK_usuperm_usu_cad   FOREIGN KEY (IDR_UsuarioCadastro)  REFERENCES usuario   (ID_Usuario),
  CONSTRAINT FK_usuperm_usu_alt   FOREIGN KEY (IDR_UsuarioAlteracao) REFERENCES usuario   (ID_Usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TRIGGER IF EXISTS TRG_usuperm_ai_audit;
DROP TRIGGER IF EXISTS TRG_usuperm_au_audit;
DROP TRIGGER IF EXISTS TRG_usuperm_bd_softdelete;

DELIMITER $$
CREATE TRIGGER TRG_usuperm_ai_audit AFTER INSERT ON usuario_permissao FOR EACH ROW
BEGIN
  CALL SP_RegistrarAuditoria('usuario_permissao', NEW.ID_UsuarioPermissao, 'INSERT', NULL,
    JSON_OBJECT('IDR_Usuario', NEW.IDR_Usuario, 'IDR_Permissao', NEW.IDR_Permissao), NEW.IDR_UsuarioCadastro);
END$$
CREATE TRIGGER TRG_usuperm_au_audit AFTER UPDATE ON usuario_permissao FOR EACH ROW
BEGIN
  IF OLD.FG_Excluido <> NEW.FG_Excluido THEN
    CALL SP_RegistrarAuditoria('usuario_permissao', NEW.ID_UsuarioPermissao, 'UPDATE',
      JSON_OBJECT('FG_Excluido', OLD.FG_Excluido), JSON_OBJECT('FG_Excluido', NEW.FG_Excluido), NEW.IDR_UsuarioAlteracao);
  END IF;
END$$
CREATE TRIGGER TRG_usuperm_bd_softdelete BEFORE DELETE ON usuario_permissao FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Exclusão física não permitida. Use soft delete (FG_Excluido = 1).';
END$$
DELIMITER ;

-- ---------------------------------------------------------------------
-- 3) Semear perfil_permissao dos perfis padrao (idempotente)
-- ---------------------------------------------------------------------
-- Administrador: todas as permissoes
INSERT INTO perfil_permissao (IDR_Perfil, IDR_Permissao)
SELECT p.ID_Perfil, pe.ID_Permissao
FROM perfil p CROSS JOIN permissao pe
WHERE p.NM_Perfil = 'Administrador' AND pe.FG_Excluido = 0
AND NOT EXISTS (SELECT 1 FROM perfil_permissao pp WHERE pp.IDR_Perfil = p.ID_Perfil AND pp.IDR_Permissao = pe.ID_Permissao);

-- Consulta: apenas leitura
INSERT INTO perfil_permissao (IDR_Perfil, IDR_Permissao)
SELECT p.ID_Perfil, pe.ID_Permissao
FROM perfil p JOIN permissao pe ON pe.NM_Acao IN ('listar','visualizar','consultar')
WHERE p.NM_Perfil = 'Consulta' AND pe.FG_Excluido = 0
AND NOT EXISTS (SELECT 1 FROM perfil_permissao pp WHERE pp.IDR_Perfil = p.ID_Perfil AND pp.IDR_Permissao = pe.ID_Permissao);

-- Operador: modulos operacionais (todas as acoes) + leitura dos demais
INSERT INTO perfil_permissao (IDR_Perfil, IDR_Permissao)
SELECT p.ID_Perfil, pe.ID_Permissao
FROM perfil p JOIN permissao pe ON (
      pe.NM_Modulo IN ('item','triagem','etiqueta','deposito','localizacao','arquivo','lacre')
   OR (pe.NM_Modulo IN ('evento','categoria','local','equipe','status','dashboard','relatorio','analytics','sla') AND pe.NM_Acao IN ('listar','visualizar')))
WHERE p.NM_Perfil = 'Operador' AND pe.FG_Excluido = 0
AND NOT EXISTS (SELECT 1 FROM perfil_permissao pp WHERE pp.IDR_Perfil = p.ID_Perfil AND pp.IDR_Permissao = pe.ID_Permissao);

-- Atendente: atendimento (todas as acoes) + leitura de itens/eventos/relatorios
INSERT INTO perfil_permissao (IDR_Perfil, IDR_Permissao)
SELECT p.ID_Perfil, pe.ID_Permissao
FROM perfil p JOIN permissao pe ON (
      pe.NM_Modulo IN ('claim','devolucao','contato','crianca')
   OR (pe.NM_Modulo IN ('item','evento','dashboard','status','relatorio') AND pe.NM_Acao IN ('listar','visualizar')))
WHERE p.NM_Perfil = 'Atendente' AND pe.FG_Excluido = 0
AND NOT EXISTS (SELECT 1 FROM perfil_permissao pp WHERE pp.IDR_Perfil = p.ID_Perfil AND pp.IDR_Permissao = pe.ID_Permissao);
