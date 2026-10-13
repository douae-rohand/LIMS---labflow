-- lims_central est codé en dur (cf. SchemaConstants.CENTRAL_SCHEMA).
-- Le DELETE des échantillons non reçus est destructif : toute ligne
-- dont date_reception est NULL est détachée de ligne_essai puis supprimée.
-- Le repli ADMINISTRATEUR ne concerne que la démo (lims_nord n'a pas d'ACCUEIL).
-- Aucun id n'est codé en dur. Si receptionneur_id reste NULL, le MODIFY échoue.

UPDATE ligne_essai le
JOIN echantillon e ON e.id_echantillon = le.echantillon_id
SET le.echantillon_id = NULL
WHERE e.date_reception IS NULL;

DELETE FROM echantillon WHERE date_reception IS NULL;

UPDATE echantillon e
SET e.receptionneur_id = (
    SELECT u.id_utilisateur
    FROM lims_central.utilisateur u
    JOIN lims_central.role r ON r.id_role = u.role_id
    JOIN lims_central.laboratoire l ON l.id_laboratoire = u.laboratoire_id
    WHERE l.nom_schema = DATABASE()
      AND r.code = 'ACCUEIL'
    LIMIT 1
)
WHERE e.receptionneur_id IS NULL;

UPDATE echantillon e
SET e.receptionneur_id = (
    SELECT u.id_utilisateur
    FROM lims_central.utilisateur u
    JOIN lims_central.role r ON r.id_role = u.role_id
    JOIN lims_central.laboratoire l ON l.id_laboratoire = u.laboratoire_id
    WHERE l.nom_schema = DATABASE()
      AND r.code = 'ADMINISTRATEUR'
    LIMIT 1
)
WHERE e.receptionneur_id IS NULL;

ALTER TABLE echantillon
    DROP COLUMN statut,
    CHANGE COLUMN conditions_conservation etat_conservation_constate TEXT NULL,
    MODIFY COLUMN nature VARCHAR(100) NOT NULL,
    MODIFY COLUMN date_reception DATETIME NOT NULL,
    MODIFY COLUMN receptionneur_id BIGINT NOT NULL;
