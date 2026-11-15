-- ============================================================
-- V32 : Commission de la plateforme et annulation des factures
--   * taux_commission : taux de commission de la plateforme figé à
--     l'émission de la facture (5 % pour les factures déjà émises) ;
--   * une facture n'est jamais supprimée : elle est annulée par un avoir
--     numéroté AV-AAAAMM-NNN, avec sa date et son motif.
-- ============================================================

ALTER TABLE facture
    ADD COLUMN taux_commission DECIMAL(5,2) NULL,
    ADD COLUMN numero_avoir VARCHAR(50) NULL,
    ADD COLUMN date_annulation TIMESTAMP NULL,
    ADD COLUMN motif_annulation VARCHAR(500) NULL,
    ADD CONSTRAINT uk_facture_numero_avoir UNIQUE (numero_avoir);

UPDATE facture SET taux_commission = 5.00;
