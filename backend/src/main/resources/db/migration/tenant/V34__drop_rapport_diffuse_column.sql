-- V34 : suppression de la colonne diffuse de la table rapport.
-- La redondance avec statut IN ('ENVOYE', 'ARCHIVE') a été identifiée ;
-- le code applicatif a été mis à jour pour dériver cette information du statut.
ALTER TABLE rapport DROP COLUMN diffuse;
