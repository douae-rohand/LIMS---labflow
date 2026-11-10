-- ============================================================
-- V39 : Identité de la plateforme et TVA
--   * clés d'identité de la plateforme (valeurs par défaut)
--   * taux de TVA de la plateforme (défaut 20 %)
--   * taux de TVA propre à un laboratoire (NULL = taux de la plateforme)
-- ============================================================

INSERT IGNORE INTO parametre_plateforme (cle, valeur) VALUES
    ('plateforme.nom', 'LabFlow'),
    ('plateforme.email_contact', ''),
    ('plateforme.telephone', ''),
    ('plateforme.adresse', ''),
    ('email.expediteur_nom', 'LabFlow LIMS'),
    ('facturation.tva.taux', '20');

ALTER TABLE laboratoire ADD COLUMN taux_tva DECIMAL(5,2) NULL;
