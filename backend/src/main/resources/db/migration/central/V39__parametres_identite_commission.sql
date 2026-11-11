-- ============================================================
-- V39 : Identité de la plateforme et commission
--   * clés d'identité de la plateforme (valeurs par défaut) ;
--   * commission de la plateforme : pourcentage prélevé sur le montant
--     de chaque facture de demande (5 % par défaut, paramétrable par le
--     Super Administrateur).
-- ============================================================

INSERT IGNORE INTO parametre_plateforme (cle, valeur) VALUES
    ('plateforme.nom', 'LabFlow'),
    ('plateforme.email_contact', ''),
    ('plateforme.telephone', ''),
    ('plateforme.adresse', ''),
    ('email.expediteur_nom', 'LabFlow LIMS'),
    ('plateforme.commission.taux', '5');
