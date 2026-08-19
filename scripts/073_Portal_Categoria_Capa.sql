-- =====================================================================
-- 073_Portal_Categoria_Capa.sql
-- Imagem substituta no portal público por categoria (ex.: Documentos).
-- Quando há capa, o portal não expõe a foto original do item.
-- Aplicar: mysql --default-character-set=utf8mb4 ... achados_perdidos < 073_Portal_Categoria_Capa.sql
-- =====================================================================

CREATE TABLE IF NOT EXISTS portal_categoria_capa (
  ID_PortalCategoriaCapa BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  IDR_Categoria BIGINT UNSIGNED NOT NULL,
  IDR_Arquivo BIGINT UNSIGNED NOT NULL,
  IDR_Evento BIGINT UNSIGNED NOT NULL,
  DT_Cadastro DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  DT_Alteracao DATETIME NULL,
  FG_Ativo TINYINT(1) NOT NULL DEFAULT 1,
  FG_Excluido TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (ID_PortalCategoriaCapa),
  UNIQUE KEY UK_portal_categoria_capa_cat (IDR_Categoria),
  KEY IX_portal_categoria_capa_arq (IDR_Arquivo),
  CONSTRAINT FK_portal_categoria_capa_cat FOREIGN KEY (IDR_Categoria) REFERENCES categoria (ID_Categoria),
  CONSTRAINT FK_portal_categoria_capa_arq FOREIGN KEY (IDR_Arquivo) REFERENCES arquivo (ID_Arquivo),
  CONSTRAINT FK_portal_categoria_capa_evt FOREIGN KEY (IDR_Evento) REFERENCES evento (ID_Evento)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
