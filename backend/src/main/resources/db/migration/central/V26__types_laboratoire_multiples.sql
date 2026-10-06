-- Un laboratoire peut être admis dans plusieurs catégories.
-- Les valeurs sont stockées en liste séparée par des virgules.

ALTER TABLE demande_integration
    MODIFY COLUMN type_laboratoire VARCHAR(500) NULL;

ALTER TABLE laboratoire
    MODIFY COLUMN type_laboratoire VARCHAR(500) NULL;
