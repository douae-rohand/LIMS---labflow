-- Enrichit la demande d'intégration, le laboratoire et l'utilisateur
-- pour le flux complet d'onboarding (wizard + documents + localisation).

ALTER TABLE demande_integration
    ADD COLUMN nom_laboratoire VARCHAR(255) NULL AFTER raison_sociale,
    ADD COLUMN type_laboratoire VARCHAR(100) NULL AFTER nom_laboratoire,
    ADD COLUMN telephone_laboratoire VARCHAR(50) NULL AFTER type_laboratoire,
    ADD COLUMN email_laboratoire VARCHAR(255) NULL AFTER telephone_laboratoire,
    ADD COLUMN site_web VARCHAR(255) NULL AFTER email_laboratoire,
    ADD COLUMN ville VARCHAR(100) NULL AFTER adresse,
    ADD COLUMN region VARCHAR(100) NULL AFTER ville,
    ADD COLUMN pays VARCHAR(100) NULL AFTER region,
    ADD COLUMN code_postal VARCHAR(20) NULL AFTER pays,
    ADD COLUMN latitude DOUBLE NULL AFTER code_postal,
    ADD COLUMN longitude DOUBLE NULL AFTER latitude,
    ADD COLUMN contact_prenom VARCHAR(100) NULL AFTER contact_nom,
    ADD COLUMN contact_fonction VARCHAR(100) NULL AFTER contact_telephone,
    ADD COLUMN contact_cin VARCHAR(20) NULL AFTER contact_fonction,
    ADD COLUMN date_traitement TIMESTAMP NULL AFTER date_demande;

UPDATE demande_integration
SET nom_laboratoire = raison_sociale
WHERE nom_laboratoire IS NULL;

ALTER TABLE laboratoire
    ADD COLUMN type_laboratoire VARCHAR(100) NULL AFTER ice,
    ADD COLUMN site_web VARCHAR(255) NULL AFTER email,
    ADD COLUMN region VARCHAR(100) NULL AFTER ville,
    ADD COLUMN pays VARCHAR(100) NULL AFTER region,
    ADD COLUMN code_postal VARCHAR(20) NULL AFTER pays;

ALTER TABLE utilisateur
    ADD COLUMN fonction VARCHAR(100) NULL AFTER cin;

CREATE TABLE document_integration (
    id_document BIGINT NOT NULL AUTO_INCREMENT,
    demande_id BIGINT NOT NULL,
    type_document VARCHAR(80) NOT NULL,
    nom_fichier VARCHAR(255) NOT NULL,
    type_mime VARCHAR(120) NOT NULL,
    taille BIGINT NOT NULL,
    chemin_stockage VARCHAR(500) NOT NULL,
    date_ajout TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_document),
    UNIQUE KEY uk_document_integration_demande_type (demande_id, type_document),
    CONSTRAINT fk_document_integration_demande
        FOREIGN KEY (demande_id) REFERENCES demande_integration (id_demande_integ)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
