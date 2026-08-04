-- =====================================================================
-- 065_Wallpaper_Download.sql
-- Contador de downloads de wallpaper do portal público (/wallpaper).
-- Cada clique em "Baixar Wallpaper (.PNG)" grava uma linha; o card
-- "Wallpapers Baixados" do /dashboard soma os registros do evento.
-- Aplicar com: mysql --default-character-set=utf8mb4 -u root achados_perdidos < 065_Wallpaper_Download.sql
-- Idempotente.
-- =====================================================================

CREATE TABLE IF NOT EXISTS `wallpaper_download` (
  `ID_WallpaperDownload` bigint unsigned NOT NULL AUTO_INCREMENT,
  `IDR_Evento`           bigint unsigned NOT NULL,
  `IDR_Arquivo`          bigint unsigned DEFAULT NULL COMMENT 'Arte escolhida (arquivo TP_Arquivo=WALLPAPER)',
  `NM_Origem`            varchar(30)  NOT NULL DEFAULT 'PORTAL' COMMENT 'Origem do download',
  `NR_Ip`                varchar(45)  DEFAULT NULL,
  `DS_UserAgent`         varchar(300) DEFAULT NULL,
  `DT_Download`          datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `DT_Cadastro`          datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `FG_Ativo`             tinyint(1)   NOT NULL DEFAULT '1',
  `FG_Excluido`          tinyint(1)   NOT NULL DEFAULT '0',
  PRIMARY KEY (`ID_WallpaperDownload`),
  KEY `IX_wallpaper_download_evento_data` (`IDR_Evento`, `DT_Download`),
  KEY `FK_wallpaper_download_arquivo` (`IDR_Arquivo`),
  CONSTRAINT `FK_wallpaper_download_evento`  FOREIGN KEY (`IDR_Evento`)  REFERENCES `evento`  (`ID_Evento`),
  CONSTRAINT `FK_wallpaper_download_arquivo` FOREIGN KEY (`IDR_Arquivo`) REFERENCES `arquivo` (`ID_Arquivo`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
