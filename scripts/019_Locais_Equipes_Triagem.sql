-- =====================================================================
-- 019_Locais_Equipes_Triagem.sql
-- Cadastros de Locais (secao 9) e Equipes (secao 10) e modulo de
-- Triagem (secao 6) da Especificacao Funcional (Rock in Rio).
-- Executar uma unica vez.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) Locais operacionais (local de achado, posto de coleta, deposito,
--    atendimento ao publico, operacional interno)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS local (
  ID_Local             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  IDR_Evento           BIGINT UNSIGNED NOT NULL,
  IDR_Responsavel      BIGINT UNSIGNED NULL,
  NM_Local             VARCHAR(150) NOT NULL,
  TP_Local             VARCHAR(40)  NOT NULL,          -- ACHADO|COLETA|DEPOSITO|ATENDIMENTO|OPERACIONAL
  VL_Latitude          DECIMAL(10,7) NULL,
  VL_Longitude         DECIMAL(10,7) NULL,
  NM_Horario           VARCHAR(120) NULL,
  DS_Observacao        VARCHAR(500) NULL,
  DT_Cadastro          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  DT_Alteracao         DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
  IDR_UsuarioCadastro  BIGINT UNSIGNED NULL,
  IDR_UsuarioAlteracao BIGINT UNSIGNED NULL,
  FG_Ativo             TINYINT(1) NOT NULL DEFAULT 1,
  FG_Excluido          TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (ID_Local),
  KEY IX_local_evento (IDR_Evento),
  KEY IX_local_tipo (TP_Local),
  CONSTRAINT FK_local_evento      FOREIGN KEY (IDR_Evento)      REFERENCES evento (ID_Evento),
  CONSTRAINT FK_local_responsavel FOREIGN KEY (IDR_Responsavel) REFERENCES usuario (ID_Usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 2) Equipes operacionais e seus membros
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS equipe (
  ID_Equipe            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  IDR_Evento           BIGINT UNSIGNED NOT NULL,
  IDR_Local            BIGINT UNSIGNED NULL,
  NM_Equipe            VARCHAR(150) NOT NULL,
  TP_Equipe            VARCHAR(40)  NOT NULL,          -- COLETA|TRIAGEM|ESTOQUE|ATENDIMENTO|SUPERVISAO|ADMINISTRACAO
  DS_Responsabilidade  VARCHAR(500) NULL,
  DT_Cadastro          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  DT_Alteracao         DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
  IDR_UsuarioCadastro  BIGINT UNSIGNED NULL,
  IDR_UsuarioAlteracao BIGINT UNSIGNED NULL,
  FG_Ativo             TINYINT(1) NOT NULL DEFAULT 1,
  FG_Excluido          TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (ID_Equipe),
  KEY IX_equipe_evento (IDR_Evento),
  KEY IX_equipe_tipo (TP_Equipe),
  CONSTRAINT FK_equipe_evento FOREIGN KEY (IDR_Evento) REFERENCES evento (ID_Evento),
  CONSTRAINT FK_equipe_local  FOREIGN KEY (IDR_Local)  REFERENCES local  (ID_Local)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS equipe_usuario (
  ID_EquipeUsuario     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  IDR_Equipe           BIGINT UNSIGNED NOT NULL,
  IDR_Usuario          BIGINT UNSIGNED NOT NULL,
  DT_Cadastro          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  DT_Alteracao         DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
  IDR_UsuarioCadastro  BIGINT UNSIGNED NULL,
  IDR_UsuarioAlteracao BIGINT UNSIGNED NULL,
  FG_Ativo             TINYINT(1) NOT NULL DEFAULT 1,
  FG_Excluido          TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (ID_EquipeUsuario),
  UNIQUE KEY UQ_equipe_usuario (IDR_Equipe, IDR_Usuario),
  CONSTRAINT FK_equipeusuario_equipe  FOREIGN KEY (IDR_Equipe)  REFERENCES equipe  (ID_Equipe),
  CONSTRAINT FK_equipeusuario_usuario FOREIGN KEY (IDR_Usuario) REFERENCES usuario (ID_Usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 3) Triagem do item (secao 6): classificacao, tags, observacoes,
--    sugestao por IA e localizacao fisica inicial. Uma triagem por item.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS triagem (
  ID_Triagem              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  IDR_Item                BIGINT UNSIGNED NOT NULL,
  IDR_Operador            BIGINT UNSIGNED NULL,
  IDR_LocalizacaoInicial  BIGINT UNSIGNED NULL,
  NM_Estado               VARCHAR(60)  NULL,
  DS_Tags                 VARCHAR(500) NULL,
  DS_Observacao           VARCHAR(1000) NULL,
  DS_SugestaoIa           VARCHAR(300) NULL,
  VL_ConfiancaIa          DECIMAL(5,2) NULL,
  TP_Status               VARCHAR(30)  NOT NULL DEFAULT 'EM_ANDAMENTO', -- EM_ANDAMENTO|CONCLUIDA
  DT_Inicio               DATETIME NULL,
  DT_Conclusao            DATETIME NULL,
  DT_Cadastro             DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  DT_Alteracao            DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
  IDR_UsuarioCadastro     BIGINT UNSIGNED NULL,
  IDR_UsuarioAlteracao    BIGINT UNSIGNED NULL,
  FG_Ativo                TINYINT(1) NOT NULL DEFAULT 1,
  FG_Excluido             TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (ID_Triagem),
  UNIQUE KEY UQ_triagem_item (IDR_Item),
  CONSTRAINT FK_triagem_item        FOREIGN KEY (IDR_Item)               REFERENCES item        (ID_Item),
  CONSTRAINT FK_triagem_operador    FOREIGN KEY (IDR_Operador)           REFERENCES usuario     (ID_Usuario),
  CONSTRAINT FK_triagem_localizacao FOREIGN KEY (IDR_LocalizacaoInicial) REFERENCES localizacao (ID_Localizacao)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
