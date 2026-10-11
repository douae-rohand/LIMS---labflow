-- ============================================================
-- V23 : Statut d'échantillon persisté + vocabulaire canonique
--       de ligne_essai.statut (StatutEssai)
-- ============================================================

-- Alias historiques écrits par la planification ou le seed de démo.
UPDATE ligne_essai SET statut = 'PLANIFIEE' WHERE statut = 'PLANIFIE';
UPDATE ligne_essai SET statut = 'TERMINE' WHERE statut = 'TERMINEE';
UPDATE ligne_essai SET statut = 'ANNULE' WHERE statut = 'ANNULEE';

ALTER TABLE echantillon
    ADD COLUMN statut VARCHAR(30) NOT NULL DEFAULT 'EN_ATTENTE_RECEPTION'
        COMMENT 'StatutEchantillon'
        AFTER date_reception;

UPDATE echantillon
SET statut = 'NON_CONFORME'
WHERE conformite = 0;

UPDATE echantillon
SET statut = 'RECEPTIONNE'
WHERE date_reception IS NOT NULL
  AND (conformite IS NULL OR conformite = 1);
