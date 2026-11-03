-- Ajoute la colonne actif à la table produit pour permettre la désactivation
-- sans supprimer le produit (nécessaire si des lots existent)
-- Idempotent : vérifie l'existence de la colonne avant de l'ajouter

SELECT COUNT(*) INTO @col_exists
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'produit'
  AND column_name = 'actif';

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE produit ADD COLUMN actif TINYINT(1) NOT NULL DEFAULT 1',
    'SELECT "Column actif already exists" AS message');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
