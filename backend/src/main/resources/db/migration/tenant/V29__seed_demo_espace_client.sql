-- ============================================================
-- V29 : Données de démonstration de l'espace client
--       (rapports, factures, enquêtes de satisfaction, synthèse IA)
--
-- Restreint aux schémas de démonstration lims_atlas / lims_nord.
-- Idempotent : chaque insertion est gardée par NOT EXISTS sur sa clé
-- fonctionnelle (numero / code / reference). Aucune DDL.
--
-- Aucun identifiant n'est codé en dur : le compte client central
-- réellement rattaché au laboratoire est résolu dynamiquement, comme
-- en V26 (receptionneur_id). Si un compte est introuvable, la colonne
-- nullable reçoit NULL sans bloquer l'insertion.
--
-- Deux demandes TERMINEE de démonstration :
--   DEMO-2026-0001 : rapport visible + synthèse IA + facture PAYEE
--                    (paiement du montant HT exact) + enquête répondue.
--   DEMO-2026-0002 : facture IMPAYEE échue (aucun paiement) + enquête
--                    non répondue.
-- (Aucune contrainte unique n'impose une facture ou une enquête par
--  demande ; les deux dernières pièces partagent donc la 2e demande.)
-- ============================================================

-- ------------------------------------------------------------
-- 0. Résolution du client de démonstration rattaché à ce schéma
--    (via la liaison centrale client_laboratoire).
-- ------------------------------------------------------------
SET @client_user_id := (
    SELECT cl.utilisateur_id
    FROM lims_central.client_laboratoire cl
    JOIN lims_central.laboratoire l ON l.id_laboratoire = cl.laboratoire_id
    WHERE l.nom_schema = DATABASE()
      AND cl.statut = 'ACTIF'
    LIMIT 1
);

SET @client_local_id := (
    SELECT cl.client_local_id
    FROM lims_central.client_laboratoire cl
    JOIN lims_central.laboratoire l ON l.id_laboratoire = cl.laboratoire_id
    WHERE l.nom_schema = DATABASE()
      AND cl.statut = 'ACTIF'
    LIMIT 1
);

-- Aligne la fiche client locale référencée par la liaison centrale sur le
-- compte central, si elle ne l'est pas déjà (aucun doublon créé).
SET @client_lie := (SELECT COUNT(*) FROM client WHERE utilisateur_id = @client_user_id);

UPDATE client
SET utilisateur_id = @client_user_id
WHERE id_client = @client_local_id
  AND DATABASE() IN ('lims_atlas', 'lims_nord')
  AND @client_user_id IS NOT NULL
  AND @client_lie = 0;

-- Repli : aucune fiche locale pour ce compte → en créer une.
INSERT INTO client (code, raison_sociale, ice, adresse, consentement_cndp, utilisateur_id)
SELECT 'CLI-DEMO', 'Clinique Al Amal', '001888777000019', '45 Rue Ibn Sina, Casablanca', 1, @client_user_id
WHERE DATABASE() IN ('lims_atlas', 'lims_nord')
  AND @client_user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM client c WHERE c.utilisateur_id = @client_user_id);

-- ------------------------------------------------------------
-- 1. Comptes du laboratoire (signataire / encaisseur / réceptionneur).
--    NULL si le compte est introuvable (colonnes nullables) ; le
--    réceptionneur retombe sur ACCUEIL puis ADMINISTRATEUR (cf. V26).
-- ------------------------------------------------------------
SET @signataire_id := (
    SELECT u.id_utilisateur
    FROM lims_central.utilisateur u
    JOIN lims_central.role r ON r.id_role = u.role_id
    JOIN lims_central.laboratoire l ON l.id_laboratoire = u.laboratoire_id
    WHERE l.nom_schema = DATABASE()
      AND r.code = 'RESPONSABLE'
    LIMIT 1
);

SET @recepteur_id := COALESCE(
    (SELECT u.id_utilisateur
     FROM lims_central.utilisateur u
     JOIN lims_central.role r ON r.id_role = u.role_id
     JOIN lims_central.laboratoire l ON l.id_laboratoire = u.laboratoire_id
     WHERE l.nom_schema = DATABASE()
       AND r.code = 'ACCUEIL'
     LIMIT 1),
    (SELECT u.id_utilisateur
     FROM lims_central.utilisateur u
     JOIN lims_central.role r ON r.id_role = u.role_id
     JOIN lims_central.laboratoire l ON l.id_laboratoire = u.laboratoire_id
     WHERE l.nom_schema = DATABASE()
       AND r.code = 'ADMINISTRATEUR'
     LIMIT 1)
);

-- ------------------------------------------------------------
-- 2. Demande DEMO-2026-0001 — dossier complet (rapport + facture payée)
-- ------------------------------------------------------------
INSERT INTO demande (numero, titre, objectif, date_soumission, statut, client_id, patient_id)
SELECT 'DEMO-2026-0001', 'Bilan biochimique — Clinique Al Amal', 'Contrôle annuel',
       DATE_SUB(NOW(), INTERVAL 20 DAY), 'TERMINEE', c.id_client, NULL
FROM client c
WHERE c.utilisateur_id = @client_user_id
  AND DATABASE() IN ('lims_atlas', 'lims_nord')
  AND NOT EXISTS (SELECT 1 FROM demande d WHERE d.numero = 'DEMO-2026-0001');

INSERT INTO echantillon (reference, nature, date_reception, conformite, motif, etat_conservation_constate, demande_id, receptionneur_id)
SELECT 'ECH-DEMO-0001', 'Sang', DATE_SUB(NOW(), INTERVAL 20 DAY), 1, NULL, '2 à 8 °C', d.id_demande, @recepteur_id
FROM demande d
WHERE d.numero = 'DEMO-2026-0001'
  AND DATABASE() IN ('lims_atlas', 'lims_nord')
  AND NOT EXISTS (SELECT 1 FROM echantillon e WHERE e.reference = 'ECH-DEMO-0001');

INSERT INTO ligne_essai (code, statut, date_attribution, date_debut, date_fin, duree_minutes, valeur, date_saisie, conformite, montant, demande_id, essai_id, echantillon_id)
SELECT 'LE-DEMO-0001', 'TERMINE', DATE_SUB(NOW(), INTERVAL 19 DAY), DATE_SUB(NOW(), INTERVAL 19 DAY),
       DATE_SUB(NOW(), INTERVAL 18 DAY), 60, '0.92', DATE_SUB(NOW(), INTERVAL 18 DAY), 1, 45.00,
       d.id_demande, e.id_essai, ech.id_echantillon
FROM demande d
JOIN essai e ON e.code = 'GLY'
JOIN echantillon ech ON ech.reference = 'ECH-DEMO-0001'
WHERE d.numero = 'DEMO-2026-0001'
  AND DATABASE() IN ('lims_atlas', 'lims_nord')
  AND NOT EXISTS (SELECT 1 FROM ligne_essai le WHERE le.code = 'LE-DEMO-0001');

INSERT INTO rapport (numero, type, version, date_emission, statut, date_signature, cle_pdf, diffuse, demande_id, signataire_id)
SELECT 'RAP-DEMO-0001', 'FINAL', 1, DATE_SUB(NOW(), INTERVAL 17 DAY), 'SIGNE',
       DATE_SUB(NOW(), INTERVAL 17 DAY), NULL, 1, d.id_demande, @signataire_id
FROM demande d
WHERE d.numero = 'DEMO-2026-0001'
  AND DATABASE() IN ('lims_atlas', 'lims_nord')
  AND NOT EXISTS (SELECT 1 FROM rapport r WHERE r.numero = 'RAP-DEMO-0001');

INSERT INTO rapport_ligne (rapport_id, ligne_essai_id)
SELECT r.id_rapport, le.id_ligne_essai
FROM rapport r
JOIN ligne_essai le ON le.code = 'LE-DEMO-0001'
WHERE r.numero = 'RAP-DEMO-0001'
  AND DATABASE() IN ('lims_atlas', 'lims_nord')
  AND NOT EXISTS (
      SELECT 1 FROM rapport_ligne rl
      WHERE rl.rapport_id = r.id_rapport AND rl.ligne_essai_id = le.id_ligne_essai
  );

INSERT INTO synthese_ia (code, contenu, date_generation, demande_id)
SELECT 'SYN-DEMO-0001',
       'Glycémie à jeun mesurée à 0,92 g/L, dans les valeurs usuelles. Aucune anomalie détectée sur cette demande.',
       DATE_SUB(NOW(), INTERVAL 18 DAY), d.id_demande
FROM demande d
WHERE d.numero = 'DEMO-2026-0001'
  AND DATABASE() IN ('lims_atlas', 'lims_nord')
  AND NOT EXISTS (SELECT 1 FROM synthese_ia s WHERE s.code = 'SYN-DEMO-0001');

-- Facture 1 : PAYEE (un paiement du montant HT exact).
INSERT INTO facture (numero, type, date_facture, date_echeance, montant_ht, statut, demande_id)
SELECT 'FAC-DEMO-0001', 'FACTURE', DATE_SUB(CURDATE(), INTERVAL 15 DAY),
       DATE_ADD(CURDATE(), INTERVAL 15 DAY), 45.00, 'PAYEE', d.id_demande
FROM demande d
WHERE d.numero = 'DEMO-2026-0001'
  AND DATABASE() IN ('lims_atlas', 'lims_nord')
  AND NOT EXISTS (SELECT 1 FROM facture f WHERE f.numero = 'FAC-DEMO-0001');

INSERT INTO facture_ligne (facture_id, ligne_essai_id, designation, montant)
SELECT f.id_facture, le.id_ligne_essai, 'Glycémie à jeun', 45.00
FROM facture f
JOIN ligne_essai le ON le.code = 'LE-DEMO-0001'
WHERE f.numero = 'FAC-DEMO-0001'
  AND DATABASE() IN ('lims_atlas', 'lims_nord')
  AND NOT EXISTS (
      SELECT 1 FROM facture_ligne fl
      WHERE fl.facture_id = f.id_facture AND fl.ligne_essai_id = le.id_ligne_essai
  );

INSERT INTO paiement (code, date_paiement, montant, mode, reference, facture_id, encaisseur_id)
SELECT 'PAY-DEMO-0001', DATE_SUB(NOW(), INTERVAL 10 DAY), 45.00, 'VIREMENT', 'VIR-DEMO-0001',
       f.id_facture, @recepteur_id
FROM facture f
WHERE f.numero = 'FAC-DEMO-0001'
  AND DATABASE() IN ('lims_atlas', 'lims_nord')
  AND NOT EXISTS (SELECT 1 FROM paiement p WHERE p.code = 'PAY-DEMO-0001');

INSERT INTO enquete_satisfaction (code, date_envoi, date_reponse, note_globale, note_delai, note_clarte, note_relation, commentaire, relance_envoyee, demande_id)
SELECT 'ENQ-DEMO-0001', DATE_SUB(NOW(), INTERVAL 14 DAY), DATE_SUB(NOW(), INTERVAL 13 DAY),
       5, 5, 4, 5, 'Prestation conforme et rapide.', 0, d.id_demande
FROM demande d
WHERE d.numero = 'DEMO-2026-0001'
  AND DATABASE() IN ('lims_atlas', 'lims_nord')
  AND NOT EXISTS (SELECT 1 FROM enquete_satisfaction e WHERE e.code = 'ENQ-DEMO-0001');

-- ------------------------------------------------------------
-- 3. Demande DEMO-2026-0002 — facture impayée échue + enquête à répondre
-- ------------------------------------------------------------
INSERT INTO demande (numero, titre, objectif, date_soumission, statut, client_id, patient_id)
SELECT 'DEMO-2026-0002', 'Contrôle qualité — Clinique Al Amal', 'Suivi de conformité',
       DATE_SUB(NOW(), INTERVAL 60 DAY), 'TERMINEE', c.id_client, NULL
FROM client c
WHERE c.utilisateur_id = @client_user_id
  AND DATABASE() IN ('lims_atlas', 'lims_nord')
  AND NOT EXISTS (SELECT 1 FROM demande d WHERE d.numero = 'DEMO-2026-0002');

INSERT INTO echantillon (reference, nature, date_reception, conformite, motif, etat_conservation_constate, demande_id, receptionneur_id)
SELECT 'ECH-DEMO-0002', 'Sang', DATE_SUB(NOW(), INTERVAL 60 DAY), 1, NULL, '2 à 8 °C', d.id_demande, @recepteur_id
FROM demande d
WHERE d.numero = 'DEMO-2026-0002'
  AND DATABASE() IN ('lims_atlas', 'lims_nord')
  AND NOT EXISTS (SELECT 1 FROM echantillon e WHERE e.reference = 'ECH-DEMO-0002');

INSERT INTO ligne_essai (code, statut, date_attribution, date_debut, date_fin, duree_minutes, valeur, date_saisie, conformite, montant, demande_id, essai_id, echantillon_id)
SELECT 'LE-DEMO-0002', 'TERMINE', DATE_SUB(NOW(), INTERVAL 59 DAY), DATE_SUB(NOW(), INTERVAL 59 DAY),
       DATE_SUB(NOW(), INTERVAL 58 DAY), 60, '2.60', DATE_SUB(NOW(), INTERVAL 58 DAY), 0, 55.00,
       d.id_demande, e.id_essai, ech.id_echantillon
FROM demande d
JOIN essai e ON e.code = 'CHOL'
JOIN echantillon ech ON ech.reference = 'ECH-DEMO-0002'
WHERE d.numero = 'DEMO-2026-0002'
  AND DATABASE() IN ('lims_atlas', 'lims_nord')
  AND NOT EXISTS (SELECT 1 FROM ligne_essai le WHERE le.code = 'LE-DEMO-0002');

-- Facture 2 : IMPAYEE, échéance dépassée, AUCUN paiement.
INSERT INTO facture (numero, type, date_facture, date_echeance, montant_ht, statut, demande_id)
SELECT 'FAC-DEMO-0002', 'FACTURE', DATE_SUB(CURDATE(), INTERVAL 45 DAY),
       DATE_SUB(CURDATE(), INTERVAL 15 DAY), 55.00, 'IMPAYEE', d.id_demande
FROM demande d
WHERE d.numero = 'DEMO-2026-0002'
  AND DATABASE() IN ('lims_atlas', 'lims_nord')
  AND NOT EXISTS (SELECT 1 FROM facture f WHERE f.numero = 'FAC-DEMO-0002');

INSERT INTO facture_ligne (facture_id, ligne_essai_id, designation, montant)
SELECT f.id_facture, le.id_ligne_essai, 'Cholestérol total', 55.00
FROM facture f
JOIN ligne_essai le ON le.code = 'LE-DEMO-0002'
WHERE f.numero = 'FAC-DEMO-0002'
  AND DATABASE() IN ('lims_atlas', 'lims_nord')
  AND NOT EXISTS (
      SELECT 1 FROM facture_ligne fl
      WHERE fl.facture_id = f.id_facture AND fl.ligne_essai_id = le.id_ligne_essai
  );

INSERT INTO enquete_satisfaction (code, date_envoi, date_reponse, note_globale, note_delai, note_clarte, note_relation, commentaire, relance_envoyee, demande_id)
SELECT 'ENQ-DEMO-0002', DATE_SUB(NOW(), INTERVAL 3 DAY), NULL, NULL, NULL, NULL, NULL, NULL, 0, d.id_demande
FROM demande d
WHERE d.numero = 'DEMO-2026-0002'
  AND DATABASE() IN ('lims_atlas', 'lims_nord')
  AND NOT EXISTS (SELECT 1 FROM enquete_satisfaction e WHERE e.code = 'ENQ-DEMO-0002');
