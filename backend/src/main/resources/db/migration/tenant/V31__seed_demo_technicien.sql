-- ============================================================
-- V31 : Seed de démonstration — Espace Technicien (CMD-13)
--
-- Restreint aux schémas lims_atlas et lims_nord uniquement.
-- Idempotent : chaque INSERT est protégé par NOT EXISTS sur sa clé
-- fonctionnelle (code). Aucune DDL.
--
-- Résolution dynamique du technicien :
--   SELECT id_utilisateur FROM lims_central.utilisateur
--   WHERE email = 'tech.atlas@labflow.ma'
--     AND laboratoire_id = (SELECT id_laboratoire FROM lims_central.laboratoire
--                           WHERE nom_schema = DATABASE())
-- Si aucun technicien trouvé → aucune insertion, sans erreur.
--
-- Aucun identifiant codé en dur.
-- N'utilise que des demandes préfixées TECH- pour ne pas perturber
-- les seeds existants (V22 : DEM-*, V29 : DEMO-*).
--
-- États couverts pour les tests :
--   1. ATTENTE_ECHANTILLON   → échantillon rattaché, conforme
--   2. ATTENTE_ECHANTILLON   → échantillon rattaché, NON conforme
--   3. EN_COURS              → pas encore de résultat saisi
--   4. EN_COURS              → résultat saisi, non encore validé
--   5. EN_COURS (EN RETARD)  → date_attribution ancienne, délai dépassé
--   6. Demande proche de EN_VALIDATION → dernière ligne à terminer
-- ============================================================

-- ---------------------------------------------------------------
-- 0. Résolution du technicien de démonstration
-- ---------------------------------------------------------------
SET @tech_id := (
    SELECT u.id_utilisateur
    FROM lims_central.utilisateur u
    JOIN lims_central.role r ON r.id_role = u.role_id
    JOIN lims_central.laboratoire l ON l.id_laboratoire = u.laboratoire_id
    WHERE l.nom_schema = DATABASE()
      AND r.code = 'TECHNICIEN'
      AND DATABASE() IN ('lims_atlas', 'lims_nord')
    LIMIT 1
);

-- Résolution du responsable (pour date_decision des demandes ACCEPTEES)
SET @resp_id := (
    SELECT u.id_utilisateur
    FROM lims_central.utilisateur u
    JOIN lims_central.role r ON r.id_role = u.role_id
    JOIN lims_central.laboratoire l ON l.id_laboratoire = u.laboratoire_id
    WHERE l.nom_schema = DATABASE()
      AND r.code = 'RESPONSABLE'
      AND DATABASE() IN ('lims_atlas', 'lims_nord')
    LIMIT 1
);

-- Résolution du réceptionneur (ACCUEIL ou ADMINISTRATEUR en repli, comme V29)
SET @recep_id := COALESCE(
    (SELECT u.id_utilisateur
     FROM lims_central.utilisateur u
     JOIN lims_central.role r ON r.id_role = u.role_id
     JOIN lims_central.laboratoire l ON l.id_laboratoire = u.laboratoire_id
     WHERE l.nom_schema = DATABASE()
       AND r.code = 'ACCUEIL'
       AND DATABASE() IN ('lims_atlas', 'lims_nord')
     LIMIT 1),
    (SELECT u.id_utilisateur
     FROM lims_central.utilisateur u
     JOIN lims_central.role r ON r.id_role = u.role_id
     JOIN lims_central.laboratoire l ON l.id_laboratoire = u.laboratoire_id
     WHERE l.nom_schema = DATABASE()
       AND r.code = 'ADMINISTRATEUR'
       AND DATABASE() IN ('lims_atlas', 'lims_nord')
     LIMIT 1)
);

-- Résolution d'un client local existant (repli sur CLI-AMAL de V22)
SET @client_id := (
    SELECT id_client FROM client
    WHERE DATABASE() IN ('lims_atlas', 'lims_nord')
    LIMIT 1
);

-- ---------------------------------------------------------------
-- Si aucun technicien trouvé → on arrête sans erreur
-- (les insertions suivantes sont toutes conditionnées à @tech_id IS NOT NULL)
-- ---------------------------------------------------------------

-- ---------------------------------------------------------------
-- 1. Demande TECH-2026-0001 — état : ACCEPTEE
--    Lignes : GLY (ATTENTE_ECHANTILLON, échantillon CONFORME)
--             CHOL (ATTENTE_ECHANTILLON, échantillon NON CONFORME)
-- ---------------------------------------------------------------
INSERT INTO demande (numero, titre, objectif, date_soumission, statut, date_decision, decideur_id, client_id, patient_id)
SELECT 'TECH-2026-0001', 'Bilan de contrôle — Démo Technicien', 'Tests espace technicien',
       DATE_SUB(NOW(), INTERVAL 3 DAY), 'ACCEPTEE', DATE_SUB(NOW(), INTERVAL 2 DAY), @resp_id, @client_id, NULL
WHERE @tech_id IS NOT NULL
  AND DATABASE() IN ('lims_atlas', 'lims_nord')
  AND NOT EXISTS (SELECT 1 FROM demande WHERE numero = 'TECH-2026-0001');

-- Échantillon CONFORME pour GLY
INSERT INTO echantillon (reference, nature, date_reception, conformite, motif, etat_conservation_constate, demande_id, receptionneur_id)
SELECT 'ECH-TECH-0001', 'Sang', DATE_SUB(NOW(), INTERVAL 2 DAY), 1, NULL, '2 à 8 °C', d.id_demande, @recep_id
FROM demande d
WHERE d.numero = 'TECH-2026-0001'
  AND @tech_id IS NOT NULL
  AND DATABASE() IN ('lims_atlas', 'lims_nord')
  AND NOT EXISTS (SELECT 1 FROM echantillon WHERE reference = 'ECH-TECH-0001');

-- Échantillon NON CONFORME pour CHOL
INSERT INTO echantillon (reference, nature, date_reception, conformite, motif, etat_conservation_constate, demande_id, receptionneur_id)
SELECT 'ECH-TECH-0002', 'Sang', DATE_SUB(NOW(), INTERVAL 2 DAY), 0,
       'Température de transport hors norme', 'Température ambiante (dégradation suspectée)',
       d.id_demande, @recep_id
FROM demande d
WHERE d.numero = 'TECH-2026-0001'
  AND @tech_id IS NOT NULL
  AND DATABASE() IN ('lims_atlas', 'lims_nord')
  AND NOT EXISTS (SELECT 1 FROM echantillon WHERE reference = 'ECH-TECH-0002');

-- Ligne 1 : GLY / ATTENTE_ECHANTILLON / échantillon CONFORME
INSERT INTO ligne_essai (code, statut, date_attribution, montant, demande_id, essai_id, echantillon_id, technicien_id)
SELECT 'LE-TECH-0001', 'ATTENTE_ECHANTILLON', DATE_SUB(NOW(), INTERVAL 2 DAY), 45.00,
       d.id_demande, e.id_essai, ech.id_echantillon, @tech_id
FROM demande d
JOIN essai e ON e.code = 'GLY'
JOIN echantillon ech ON ech.reference = 'ECH-TECH-0001'
WHERE d.numero = 'TECH-2026-0001'
  AND @tech_id IS NOT NULL
  AND DATABASE() IN ('lims_atlas', 'lims_nord')
  AND NOT EXISTS (SELECT 1 FROM ligne_essai WHERE code = 'LE-TECH-0001');

-- Ligne 2 : CHOL / ATTENTE_ECHANTILLON / échantillon NON CONFORME
INSERT INTO ligne_essai (code, statut, date_attribution, montant, demande_id, essai_id, echantillon_id, technicien_id)
SELECT 'LE-TECH-0002', 'ATTENTE_ECHANTILLON', DATE_SUB(NOW(), INTERVAL 2 DAY), 55.00,
       d.id_demande, e.id_essai, ech.id_echantillon, @tech_id
FROM demande d
JOIN essai e ON e.code = 'CHOL'
JOIN echantillon ech ON ech.reference = 'ECH-TECH-0002'
WHERE d.numero = 'TECH-2026-0001'
  AND @tech_id IS NOT NULL
  AND DATABASE() IN ('lims_atlas', 'lims_nord')
  AND NOT EXISTS (SELECT 1 FROM ligne_essai WHERE code = 'LE-TECH-0002');

-- ---------------------------------------------------------------
-- 2. Demande TECH-2026-0002 — état : EN_COURS
--    Ligne : NFS (EN_COURS, résultat non saisi)
--    Ligne : EAU-PH (EN_COURS, résultat saisi, non validé)
-- ---------------------------------------------------------------
INSERT INTO demande (numero, titre, objectif, date_soumission, statut, date_decision, decideur_id, client_id, patient_id)
SELECT 'TECH-2026-0002', 'Analyses en cours — Démo Technicien', 'Tests saisie résultats',
       DATE_SUB(NOW(), INTERVAL 5 DAY), 'EN_COURS', DATE_SUB(NOW(), INTERVAL 4 DAY), @resp_id, @client_id, NULL
WHERE @tech_id IS NOT NULL
  AND DATABASE() IN ('lims_atlas', 'lims_nord')
  AND NOT EXISTS (SELECT 1 FROM demande WHERE numero = 'TECH-2026-0002');

INSERT INTO echantillon (reference, nature, date_reception, conformite, motif, etat_conservation_constate, demande_id, receptionneur_id)
SELECT 'ECH-TECH-0003', 'Sang', DATE_SUB(NOW(), INTERVAL 4 DAY), 1, NULL, '2 à 8 °C', d.id_demande, @recep_id
FROM demande d
WHERE d.numero = 'TECH-2026-0002'
  AND @tech_id IS NOT NULL
  AND DATABASE() IN ('lims_atlas', 'lims_nord')
  AND NOT EXISTS (SELECT 1 FROM echantillon WHERE reference = 'ECH-TECH-0003');

-- NFS : EN_COURS, pas de résultat
INSERT INTO ligne_essai (code, statut, date_attribution, date_debut, montant, demande_id, essai_id, echantillon_id, technicien_id)
SELECT 'LE-TECH-0003', 'EN_COURS', DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY),
       80.00, d.id_demande, e.id_essai, ech.id_echantillon, @tech_id
FROM demande d
JOIN essai e ON e.code = 'NFS'
JOIN echantillon ech ON ech.reference = 'ECH-TECH-0003'
WHERE d.numero = 'TECH-2026-0002'
  AND @tech_id IS NOT NULL
  AND DATABASE() IN ('lims_atlas', 'lims_nord')
  AND NOT EXISTS (SELECT 1 FROM ligne_essai WHERE code = 'LE-TECH-0003');

-- EAU-PH : EN_COURS, résultat saisi (6.80 = CONFORME), non validé
INSERT INTO ligne_essai (code, statut, date_attribution, date_debut, valeur, date_saisie, conformite, montant, demande_id, essai_id, echantillon_id, technicien_id)
SELECT 'LE-TECH-0004', 'EN_COURS', DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY),
       '6.80', DATE_SUB(NOW(), INTERVAL 1 DAY), 1, 35.00,
       d.id_demande, e.id_essai, ech.id_echantillon, @tech_id
FROM demande d
JOIN essai e ON e.code = 'EAU-PH'
JOIN echantillon ech ON ech.reference = 'ECH-TECH-0003'
WHERE d.numero = 'TECH-2026-0002'
  AND @tech_id IS NOT NULL
  AND DATABASE() IN ('lims_atlas', 'lims_nord')
  AND NOT EXISTS (SELECT 1 FROM ligne_essai WHERE code = 'LE-TECH-0004');

-- ---------------------------------------------------------------
-- 3. Demande TECH-2026-0003 — EN RETARD
--    Ligne : GLY / ATTENTE_ECHANTILLON / date_attribution très ancienne
-- ---------------------------------------------------------------
INSERT INTO demande (numero, titre, objectif, date_soumission, statut, date_decision, decideur_id, client_id, patient_id)
SELECT 'TECH-2026-0003', 'Essai en retard — Démo Technicien', 'Test indicateur enRetard',
       DATE_SUB(NOW(), INTERVAL 10 DAY), 'EN_COURS', DATE_SUB(NOW(), INTERVAL 9 DAY), @resp_id, @client_id, NULL
WHERE @tech_id IS NOT NULL
  AND DATABASE() IN ('lims_atlas', 'lims_nord')
  AND NOT EXISTS (SELECT 1 FROM demande WHERE numero = 'TECH-2026-0003');

INSERT INTO echantillon (reference, nature, date_reception, conformite, motif, etat_conservation_constate, demande_id, receptionneur_id)
SELECT 'ECH-TECH-0004', 'Urine', DATE_SUB(NOW(), INTERVAL 9 DAY), 1, NULL, 'Réfrigéré', d.id_demande, @recep_id
FROM demande d
WHERE d.numero = 'TECH-2026-0003'
  AND @tech_id IS NOT NULL
  AND DATABASE() IN ('lims_atlas', 'lims_nord')
  AND NOT EXISTS (SELECT 1 FROM echantillon WHERE reference = 'ECH-TECH-0004');

-- date_attribution = 9 jours, duree_estimee GLY = 60 min → en retard depuis longtemps
INSERT INTO ligne_essai (code, statut, date_attribution, montant, demande_id, essai_id, echantillon_id, technicien_id)
SELECT 'LE-TECH-0005', 'ATTENTE_ECHANTILLON', DATE_SUB(NOW(), INTERVAL 9 DAY),
       45.00, d.id_demande, e.id_essai, ech.id_echantillon, @tech_id
FROM demande d
JOIN essai e ON e.code = 'GLY'
JOIN echantillon ech ON ech.reference = 'ECH-TECH-0004'
WHERE d.numero = 'TECH-2026-0003'
  AND @tech_id IS NOT NULL
  AND DATABASE() IN ('lims_atlas', 'lims_nord')
  AND NOT EXISTS (SELECT 1 FROM ligne_essai WHERE code = 'LE-TECH-0005');

-- ---------------------------------------------------------------
-- 4. Demande TECH-2026-0004 — prête pour EN_VALIDATION
--    3 lignes dont 2 déjà TERMINE, 1 EN_COURS (la dernière)
-- ---------------------------------------------------------------
INSERT INTO demande (numero, titre, objectif, date_soumission, statut, date_decision, decideur_id, client_id, patient_id)
SELECT 'TECH-2026-0004', 'Bilan presque terminé — Démo Technicien', 'Test transition EN_VALIDATION',
       DATE_SUB(NOW(), INTERVAL 7 DAY), 'EN_COURS', DATE_SUB(NOW(), INTERVAL 6 DAY), @resp_id, @client_id, NULL
WHERE @tech_id IS NOT NULL
  AND DATABASE() IN ('lims_atlas', 'lims_nord')
  AND NOT EXISTS (SELECT 1 FROM demande WHERE numero = 'TECH-2026-0004');

INSERT INTO echantillon (reference, nature, date_reception, conformite, motif, etat_conservation_constate, demande_id, receptionneur_id)
SELECT 'ECH-TECH-0005', 'Sang', DATE_SUB(NOW(), INTERVAL 6 DAY), 1, NULL, '2 à 8 °C', d.id_demande, @recep_id
FROM demande d
WHERE d.numero = 'TECH-2026-0004'
  AND @tech_id IS NOT NULL
  AND DATABASE() IN ('lims_atlas', 'lims_nord')
  AND NOT EXISTS (SELECT 1 FROM echantillon WHERE reference = 'ECH-TECH-0005');

-- Ligne 6a : GLY / TERMINE (déjà validée)
INSERT INTO ligne_essai (code, statut, date_attribution, date_debut, date_fin, valeur, date_saisie, conformite, duree_minutes, montant, demande_id, essai_id, echantillon_id, technicien_id)
SELECT 'LE-TECH-0006', 'TERMINE', DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY),
       DATE_SUB(NOW(), INTERVAL 5 DAY), '0.95', DATE_SUB(NOW(), INTERVAL 5 DAY), 1, 55,
       45.00, d.id_demande, e.id_essai, ech.id_echantillon, @tech_id
FROM demande d
JOIN essai e ON e.code = 'GLY'
JOIN echantillon ech ON ech.reference = 'ECH-TECH-0005'
WHERE d.numero = 'TECH-2026-0004'
  AND @tech_id IS NOT NULL
  AND DATABASE() IN ('lims_atlas', 'lims_nord')
  AND NOT EXISTS (SELECT 1 FROM ligne_essai WHERE code = 'LE-TECH-0006');

-- Ligne 6b : CULT / NON_REALISABLE
INSERT INTO ligne_essai (code, statut, date_attribution, motif, montant, demande_id, essai_id, echantillon_id, technicien_id)
SELECT 'LE-TECH-0007', 'NON_REALISABLE', DATE_SUB(NOW(), INTERVAL 6 DAY),
       'Échantillon insuffisant pour culture', 120.00,
       d.id_demande, e.id_essai, ech.id_echantillon, @tech_id
FROM demande d
JOIN essai e ON e.code = 'CULT'
JOIN echantillon ech ON ech.reference = 'ECH-TECH-0005'
WHERE d.numero = 'TECH-2026-0004'
  AND @tech_id IS NOT NULL
  AND DATABASE() IN ('lims_atlas', 'lims_nord')
  AND NOT EXISTS (SELECT 1 FROM ligne_essai WHERE code = 'LE-TECH-0007');

-- Ligne 6c : CHOL / EN_COURS (résultat saisi, non validé — DERNIÈRE À TERMINER)
INSERT INTO ligne_essai (code, statut, date_attribution, date_debut, valeur, date_saisie, conformite, montant, demande_id, essai_id, echantillon_id, technicien_id)
SELECT 'LE-TECH-0008', 'EN_COURS', DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY),
       '1.90', DATE_SUB(NOW(), INTERVAL 4 DAY), 1, 55.00,
       d.id_demande, e.id_essai, ech.id_echantillon, @tech_id
FROM demande d
JOIN essai e ON e.code = 'CHOL'
JOIN echantillon ech ON ech.reference = 'ECH-TECH-0005'
WHERE d.numero = 'TECH-2026-0004'
  AND @tech_id IS NOT NULL
  AND DATABASE() IN ('lims_atlas', 'lims_nord')
  AND NOT EXISTS (SELECT 1 FROM ligne_essai WHERE code = 'LE-TECH-0008');
