-- Suppression de la colonne type de la table rapport
-- Un seul type de rapport existe (rapport final), cette colonne n'est plus nécessaire
ALTER TABLE rapport DROP COLUMN `type`;
