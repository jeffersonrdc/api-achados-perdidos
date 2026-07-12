-- =====================================================================
-- 020_Etiqueta_Impressao.sql
-- Registro de impressao/reimpressao de etiqueta (impressora Bluetooth),
-- conforme a secao 5 da Especificacao Funcional (Rock in Rio).
-- Cada impressao fica registrada na linha do tempo de etiquetas do item.
-- Executar uma unica vez.
-- =====================================================================
CREATE TABLE IF NOT EXISTS etiqueta_impressao (
  ID_EtiquetaImpressao BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  IDR_Item             BIGINT UNSIGNED NOT NULL,
  IDR_Operador         BIGINT UNSIGNED NULL,
  TP_Impressao         VARCHAR(20)  NOT NULL DEFAULT 'IMPRESSAO', -- IMPRESSAO|REIMPRESSAO
  NM_Impressora        VARCHAR(120) NULL,                          -- ex.: Zebra ZQ320
  NR_Identificador     VARCHAR(60)  NULL,                          -- MAC/serial bluetooth
  DS_Motivo            VARCHAR(300) NULL,                          -- motivo da reimpressao
  DT_Impressao         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  DT_Cadastro          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  DT_Alteracao         DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
  IDR_UsuarioCadastro  BIGINT UNSIGNED NULL,
  IDR_UsuarioAlteracao BIGINT UNSIGNED NULL,
  FG_Ativo             TINYINT(1) NOT NULL DEFAULT 1,
  FG_Excluido          TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (ID_EtiquetaImpressao),
  KEY IX_etiqueta_item (IDR_Item),
  CONSTRAINT FK_etiqueta_item     FOREIGN KEY (IDR_Item)     REFERENCES item    (ID_Item),
  CONSTRAINT FK_etiqueta_operador FOREIGN KEY (IDR_Operador) REFERENCES usuario (ID_Usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
