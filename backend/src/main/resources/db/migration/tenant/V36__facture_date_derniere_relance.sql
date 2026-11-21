SET @col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'facture'
      AND COLUMN_NAME = 'date_derniere_relance'
);
SET @sql := IF(@col = 0,
    'ALTER TABLE facture ADD COLUMN date_derniere_relance DATE NULL',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
