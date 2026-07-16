-- =====================================================================
-- 034_Seed_Modelos.sql
-- Carga ampla de marcas e modelos para os selects de Coleta/Triagem.
-- Idempotente: usa ON DUPLICATE KEY / INSERT ... SELECT com JOIN em marca.
-- Aplicar com: mysql --default-character-set=utf8mb4
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) Marcas adicionais (as já existentes são ignoradas pelo UNIQUE)
-- ---------------------------------------------------------------------
INSERT INTO marca (NM_Marca, OR_Ordem) VALUES
  ('Realme', 18), ('Acer', 19), ('HP', 20), ('GoPro', 21), ('Canon', 22),
  ('Nikon', 23), ('Bose', 24), ('Beats', 25), ('Anker', 26), ('Casio', 27),
  ('Michael Kors', 28), ('Amazfit', 29), ('TP-Link', 30), ('Positivo', 31),
  ('Multilaser', 32), ('Havaianas', 33), ('New Era', 34), ('Puma', 35),
  ('Vans', 36), ('Chilli Beans', 37)
ON DUPLICATE KEY UPDATE OR_Ordem = LEAST(marca.OR_Ordem, VALUES(OR_Ordem));

-- ---------------------------------------------------------------------
-- 2) Modelos por marca
-- ---------------------------------------------------------------------
INSERT INTO modelo (IDR_Marca, NM_Modelo, OR_Ordem)
SELECT m.ID_Marca, x.NM_Modelo, x.OR_Ordem
FROM (
  -- Apple
  SELECT 'Apple' AS NM_Marca, 'iPhone SE' AS NM_Modelo, 10 AS OR_Ordem UNION ALL
  SELECT 'Apple','iPhone XR',11 UNION ALL
  SELECT 'Apple','iPhone 16',12 UNION ALL
  SELECT 'Apple','iPhone 16 Pro',13 UNION ALL
  SELECT 'Apple','AirPods 2',14 UNION ALL
  SELECT 'Apple','AirPods 3',15 UNION ALL
  SELECT 'Apple','AirPods Max',16 UNION ALL
  SELECT 'Apple','Apple Watch SE',17 UNION ALL
  SELECT 'Apple','Apple Watch Series 9',18 UNION ALL
  SELECT 'Apple','Apple Watch Ultra',19 UNION ALL
  SELECT 'Apple','iPad',20 UNION ALL
  SELECT 'Apple','iPad Air',21 UNION ALL
  SELECT 'Apple','iPad Pro',22 UNION ALL
  SELECT 'Apple','MacBook Air',23 UNION ALL
  SELECT 'Apple','MacBook Pro',24 UNION ALL
  -- Samsung
  SELECT 'Samsung','Galaxy S24',10 UNION ALL
  SELECT 'Samsung','Galaxy S24 Ultra',11 UNION ALL
  SELECT 'Samsung','Galaxy A15',12 UNION ALL
  SELECT 'Samsung','Galaxy A34',13 UNION ALL
  SELECT 'Samsung','Galaxy A55',14 UNION ALL
  SELECT 'Samsung','Galaxy Z Flip 5',15 UNION ALL
  SELECT 'Samsung','Galaxy Z Fold 5',16 UNION ALL
  SELECT 'Samsung','Galaxy Buds 2',17 UNION ALL
  SELECT 'Samsung','Galaxy Buds Pro',18 UNION ALL
  SELECT 'Samsung','Galaxy Watch 6',19 UNION ALL
  SELECT 'Samsung','Galaxy Tab S9',20 UNION ALL
  -- Xiaomi
  SELECT 'Xiaomi','Redmi Note 11',10 UNION ALL
  SELECT 'Xiaomi','Redmi Note 14',11 UNION ALL
  SELECT 'Xiaomi','Redmi 13C',12 UNION ALL
  SELECT 'Xiaomi','Poco X6',13 UNION ALL
  SELECT 'Xiaomi','Poco F6',14 UNION ALL
  SELECT 'Xiaomi','Mi Band 8',15 UNION ALL
  SELECT 'Xiaomi','Redmi Buds',16 UNION ALL
  SELECT 'Xiaomi','Redmi Watch',17 UNION ALL
  -- Motorola
  SELECT 'Motorola','Moto G23',10 UNION ALL
  SELECT 'Motorola','Moto G54',11 UNION ALL
  SELECT 'Motorola','Moto G34',12 UNION ALL
  SELECT 'Motorola','Edge 50',13 UNION ALL
  SELECT 'Motorola','Razr 40',14 UNION ALL
  SELECT 'Motorola','Moto E13',15 UNION ALL
  -- Realme
  SELECT 'Realme','Realme C55',1 UNION ALL
  SELECT 'Realme','Realme 11',2 UNION ALL
  SELECT 'Realme','Realme 12 Pro',3 UNION ALL
  SELECT 'Realme','Realme Buds',4 UNION ALL
  -- LG
  SELECT 'LG','Tone Free',1 UNION ALL
  SELECT 'LG','K62',2 UNION ALL
  SELECT 'LG','Velvet',3 UNION ALL
  -- Sony
  SELECT 'Sony','WH-CH520',10 UNION ALL
  SELECT 'Sony','WF-C700N',11 UNION ALL
  SELECT 'Sony','LinkBuds',12 UNION ALL
  SELECT 'Sony','Xperia 5',13 UNION ALL
  SELECT 'Sony','Alpha A6400',14 UNION ALL
  -- JBL
  SELECT 'JBL','Charge 5',10 UNION ALL
  SELECT 'JBL','Clip 4',11 UNION ALL
  SELECT 'JBL','Boombox 3',12 UNION ALL
  SELECT 'JBL','Wave Buds',13 UNION ALL
  SELECT 'JBL','Tune 520BT',14 UNION ALL
  SELECT 'JBL','Live 660NC',15 UNION ALL
  -- Bose
  SELECT 'Bose','QuietComfort 45',1 UNION ALL
  SELECT 'Bose','QuietComfort Ultra',2 UNION ALL
  SELECT 'Bose','SoundLink Flex',3 UNION ALL
  SELECT 'Bose','Ultra Open Earbuds',4 UNION ALL
  -- Beats
  SELECT 'Beats','Studio Buds',1 UNION ALL
  SELECT 'Beats','Fit Pro',2 UNION ALL
  SELECT 'Beats','Solo 4',3 UNION ALL
  SELECT 'Beats','Powerbeats Pro',4 UNION ALL
  -- Anker
  SELECT 'Anker','Soundcore Life',1 UNION ALL
  SELECT 'Anker','PowerCore 10000',2 UNION ALL
  SELECT 'Anker','PowerCore 20000',3 UNION ALL
  SELECT 'Anker','Soundcore Motion',4 UNION ALL
  -- Ray-Ban
  SELECT 'Ray-Ban','Clubmaster',10 UNION ALL
  SELECT 'Ray-Ban','Round',11 UNION ALL
  SELECT 'Ray-Ban','Justin',12 UNION ALL
  SELECT 'Ray-Ban','Hexagonal',13 UNION ALL
  SELECT 'Ray-Ban','Meta Wayfarer',14 UNION ALL
  -- Oakley
  SELECT 'Oakley','Holbrook',1 UNION ALL
  SELECT 'Oakley','Radar EV',2 UNION ALL
  SELECT 'Oakley','Frogskins',3 UNION ALL
  SELECT 'Oakley','Flak 2.0',4 UNION ALL
  -- Chilli Beans
  SELECT 'Chilli Beans','Quadrado',1 UNION ALL
  SELECT 'Chilli Beans','Aviador',2 UNION ALL
  SELECT 'Chilli Beans','Redondo',3 UNION ALL
  -- Garmin
  SELECT 'Garmin','Forerunner 265',10 UNION ALL
  SELECT 'Garmin','Fenix 7',11 UNION ALL
  SELECT 'Garmin','Instinct 2',12 UNION ALL
  SELECT 'Garmin','Vivoactive 5',13 UNION ALL
  -- Amazfit
  SELECT 'Amazfit','GTR 4',1 UNION ALL
  SELECT 'Amazfit','GTS 4',2 UNION ALL
  SELECT 'Amazfit','Bip 5',3 UNION ALL
  SELECT 'Amazfit','Band 7',4 UNION ALL
  -- Casio
  SELECT 'Casio','G-Shock',1 UNION ALL
  SELECT 'Casio','Edifice',2 UNION ALL
  SELECT 'Casio','Vintage',3 UNION ALL
  -- Fossil
  SELECT 'Fossil','Gen 6',1 UNION ALL
  SELECT 'Fossil','Grant',2 UNION ALL
  SELECT 'Fossil','Machine',3 UNION ALL
  -- Huawei
  SELECT 'Huawei','Watch GT 4',1 UNION ALL
  SELECT 'Huawei','FreeBuds Pro',2 UNION ALL
  SELECT 'Huawei','Band 8',3 UNION ALL
  -- Asus
  SELECT 'Asus','ROG Phone 8',10 UNION ALL
  SELECT 'Asus','Zenfone 10',11 UNION ALL
  SELECT 'Asus','VivoBook 15',12 UNION ALL
  SELECT 'Asus','ROG Zephyrus',13 UNION ALL
  -- Dell
  SELECT 'Dell','Inspiron 15',10 UNION ALL
  SELECT 'Dell','XPS 13',11 UNION ALL
  SELECT 'Dell','Latitude 5440',12 UNION ALL
  -- Lenovo
  SELECT 'Lenovo','IdeaPad 3',10 UNION ALL
  SELECT 'Lenovo','ThinkPad X1',11 UNION ALL
  SELECT 'Lenovo','Legion 5',12 UNION ALL
  SELECT 'Lenovo','Tab M10',13 UNION ALL
  -- Acer
  SELECT 'Acer','Aspire 5',1 UNION ALL
  SELECT 'Acer','Nitro 5',2 UNION ALL
  SELECT 'Acer','Predator Helios',3 UNION ALL
  -- HP
  SELECT 'HP','Pavilion 15',1 UNION ALL
  SELECT 'HP','EliteBook 840',2 UNION ALL
  SELECT 'HP','Victus 16',3 UNION ALL
  -- GoPro
  SELECT 'GoPro','Hero 11',1 UNION ALL
  SELECT 'GoPro','Hero 12',2 UNION ALL
  SELECT 'GoPro','Hero 13',3 UNION ALL
  -- Canon
  SELECT 'Canon','EOS R50',1 UNION ALL
  SELECT 'Canon','EOS Rebel T7',2 UNION ALL
  SELECT 'Canon','PowerShot G7X',3 UNION ALL
  -- Nikon
  SELECT 'Nikon','D3500',1 UNION ALL
  SELECT 'Nikon','Z50',2 UNION ALL
  SELECT 'Nikon','Coolpix',3 UNION ALL
  -- New Era
  SELECT 'New Era','9FIFTY',1 UNION ALL
  SELECT 'New Era','9FORTY',2 UNION ALL
  SELECT 'New Era','59FIFTY',3 UNION ALL
  -- Nike
  SELECT 'Nike','Boné Dri-FIT',1 UNION ALL
  SELECT 'Nike','Mochila Brasilia',2 UNION ALL
  SELECT 'Nike','Pochete Heritage',3 UNION ALL
  -- Adidas
  SELECT 'Adidas','Boné Trefoil',1 UNION ALL
  SELECT 'Adidas','Mochila Classic',2 UNION ALL
  SELECT 'Adidas','Shoulder Bag',3 UNION ALL
  -- Puma
  SELECT 'Puma','Boné Archive',1 UNION ALL
  SELECT 'Puma','Mochila Phase',2 UNION ALL
  -- Multilaser / Positivo (populares e comuns em eventos)
  SELECT 'Multilaser','Power Bank',1 UNION ALL
  SELECT 'Multilaser','Fone Bluetooth',2 UNION ALL
  SELECT 'Positivo','Smartphone Twist',1 UNION ALL
  SELECT 'Positivo','Tablet',2
) x
JOIN marca m ON m.NM_Marca = x.NM_Marca
ON DUPLICATE KEY UPDATE modelo.OR_Ordem = LEAST(modelo.OR_Ordem, VALUES(OR_Ordem));
