-- ============================================================
-- V27 : Ajout des colonnes d'audit date_creation / date_modification
--        à la table utilisateur
-- ============================================================

ALTER TABLE lims_central.utilisateur
    ADD COLUMN date_creation    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP           AFTER cin,
    ADD COLUMN date_modification TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                                ON UPDATE CURRENT_TIMESTAMP                             AFTER date_creation;
