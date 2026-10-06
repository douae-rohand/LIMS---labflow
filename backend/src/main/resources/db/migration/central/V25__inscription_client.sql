-- ============================================================
-- V24 : Inscription client publique
--
-- 1. utilisateur : compte_confirme (rattrapage DEFAULT 1)
-- 2. client_profil : raison_sociale nullable, type_client,
--                    date_consentement_cndp, version_consentement_cndp
-- ============================================================

-- ----------------------------------------------------------
-- 1. Colonne compte_confirme sur utilisateur
--    DEFAULT 1 → tous les comptes existants (employés, super-admin)
--    sont considérés confirmés d'emblée.
--    L'inscription publique sera la seule à insérer 0.
-- ----------------------------------------------------------
ALTER TABLE lims_central.utilisateur
    ADD COLUMN compte_confirme TINYINT(1) NOT NULL DEFAULT 1
        COMMENT '0 = en attente de confirmation par email ; 1 = confirmé'
        AFTER actif;

-- ----------------------------------------------------------
-- 2. client_profil : raison_sociale passe en nullable
--    (les particuliers n'en ont pas)
-- ----------------------------------------------------------
ALTER TABLE lims_central.client_profil
    MODIFY COLUMN raison_sociale VARCHAR(255) NULL;

-- ----------------------------------------------------------
-- 3. type_client avec rattrapage :
--    ENTREPRISE si raison_sociale était renseignée, PARTICULIER sinon
-- ----------------------------------------------------------
ALTER TABLE lims_central.client_profil
    ADD COLUMN type_client VARCHAR(20) NOT NULL DEFAULT 'PARTICULIER'
        COMMENT 'PARTICULIER | ENTREPRISE'
        AFTER raison_sociale;

UPDATE lims_central.client_profil
SET type_client = 'ENTREPRISE'
WHERE raison_sociale IS NOT NULL AND raison_sociale <> '';

-- ----------------------------------------------------------
-- 4. Traçabilité du consentement CNDP
-- ----------------------------------------------------------
ALTER TABLE lims_central.client_profil
    ADD COLUMN date_consentement_cndp DATETIME NULL
        COMMENT 'Horodatage du consentement CNDP au moment de l''inscription'
        AFTER consentement_cndp,
    ADD COLUMN version_consentement_cndp VARCHAR(20) NULL
        COMMENT 'Version de la politique de confidentialité acceptée (ex. 1.0)'
        AFTER date_consentement_cndp;

-- Renseigner la date pour les profils déjà consentants
UPDATE lims_central.client_profil
SET date_consentement_cndp = date_creation
WHERE consentement_cndp = 1
  AND date_consentement_cndp IS NULL;
