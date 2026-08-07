-- =====================================================================
-- 067_Item_Wallpaper.sql
-- Descrição do wallpaper do celular/tablet no item coletado (/itens).
-- Executar uma única vez.
-- =====================================================================

ALTER TABLE item
  ADD COLUMN DS_Wallpaper VARCHAR(300) NULL
    COMMENT 'Descrição do papel de parede / tela de bloqueio (celulares/tablets)'
    AFTER DS_Item;
