-- ============================================================
-- V32 : TVA figée sur la facture
--   Les prix du catalogue sont saisis TTC : facture.montant_ht porte en réalité
--   le total TTC des essais. taux_tva conserve le taux en vigueur à l'émission ;
--   la part de TVA vaut montant × taux / (100 + taux).
--   NULL pour les factures émises avant cette version (taux courant du laboratoire).
-- ============================================================

ALTER TABLE facture ADD COLUMN taux_tva DECIMAL(5,2) NULL;
