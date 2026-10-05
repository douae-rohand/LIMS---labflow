-- Données de démonstration appliquées à chaque schéma tenant provisionné.

INSERT INTO domaine (code, libelle)
SELECT 'BIOCHIMIE', 'Biochimie'
WHERE NOT EXISTS (SELECT 1 FROM domaine WHERE code = 'BIOCHIMIE');

INSERT INTO domaine (code, libelle)
SELECT 'HEMATOLOGIE', 'Hématologie'
WHERE NOT EXISTS (SELECT 1 FROM domaine WHERE code = 'HEMATOLOGIE');

INSERT INTO domaine (code, libelle)
SELECT 'MICROBIOLOGIE', 'Microbiologie'
WHERE NOT EXISTS (SELECT 1 FROM domaine WHERE code = 'MICROBIOLOGIE');

INSERT INTO domaine (code, libelle)
SELECT 'ENVIRONNEMENT', 'Environnement'
WHERE NOT EXISTS (SELECT 1 FROM domaine WHERE code = 'ENVIRONNEMENT');

INSERT INTO essai (code, designation, description, methode, tarif, duree_estimee, unite, limite_min, limite_max, actif, domaine_id)
SELECT 'GLY', 'Glycémie à jeun', 'Dosage du glucose sanguin', 'Enzymatique', 45.00, 60, 'g/L', 0.7000, 1.1000, 1, d.id_domaine
FROM domaine d
WHERE d.code = 'BIOCHIMIE'
  AND NOT EXISTS (SELECT 1 FROM essai WHERE code = 'GLY');

INSERT INTO essai (code, designation, description, methode, tarif, duree_estimee, unite, limite_min, limite_max, actif, domaine_id)
SELECT 'CHOL', 'Cholestérol total', 'Dosage du cholestérol', 'Enzymatique', 55.00, 60, 'g/L', 1.5000, 2.5000, 1, d.id_domaine
FROM domaine d
WHERE d.code = 'BIOCHIMIE'
  AND NOT EXISTS (SELECT 1 FROM essai WHERE code = 'CHOL');

INSERT INTO essai (code, designation, description, methode, tarif, duree_estimee, unite, limite_min, limite_max, actif, domaine_id)
SELECT 'NFS', 'Numération formule sanguine', 'Hémogramme complet', 'Automate hématologique', 80.00, 45, 'unités', NULL, NULL, 1, d.id_domaine
FROM domaine d
WHERE d.code = 'HEMATOLOGIE'
  AND NOT EXISTS (SELECT 1 FROM essai WHERE code = 'NFS');

INSERT INTO essai (code, designation, description, methode, tarif, duree_estimee, unite, limite_min, limite_max, actif, domaine_id)
SELECT 'CULT', 'Coproculture', 'Recherche de pathogènes intestinaux', 'Culture', 120.00, 180, 'qualitatif', NULL, NULL, 1, d.id_domaine
FROM domaine d
WHERE d.code = 'MICROBIOLOGIE'
  AND NOT EXISTS (SELECT 1 FROM essai WHERE code = 'CULT');

INSERT INTO essai (code, designation, description, methode, tarif, duree_estimee, unite, limite_min, limite_max, actif, domaine_id)
SELECT 'EAU-PH', 'pH de l''eau', 'Mesure du pH d''un échantillon d''eau', 'Potentiométrie', 35.00, 30, 'pH', 6.5000, 8.5000, 1, d.id_domaine
FROM domaine d
WHERE d.code = 'ENVIRONNEMENT'
  AND NOT EXISTS (SELECT 1 FROM essai WHERE code = 'EAU-PH');

INSERT INTO produit (reference, nom, unite, seuil_minimal, conditions_stockage)
SELECT 'REAC-GLU', 'Réactif glucose', 'mL', 50.000, '2-8 °C'
WHERE NOT EXISTS (SELECT 1 FROM produit WHERE reference = 'REAC-GLU');

INSERT INTO produit (reference, nom, unite, seuil_minimal, conditions_stockage)
SELECT 'TUBE-SEC', 'Tubes secs 5 mL', 'unité', 100.000, 'Température ambiante'
WHERE NOT EXISTS (SELECT 1 FROM produit WHERE reference = 'TUBE-SEC');

INSERT INTO lot (numero, quantite, peremption, statut, produit_id)
SELECT 'LOT-GLU-01', 200.000, DATE_ADD(CURDATE(), INTERVAL 18 MONTH), 'DISPONIBLE', p.id_produit
FROM produit p
WHERE p.reference = 'REAC-GLU'
  AND NOT EXISTS (SELECT 1 FROM lot WHERE numero = 'LOT-GLU-01' AND produit_id = p.id_produit);

INSERT INTO lot (numero, quantite, peremption, statut, produit_id)
SELECT 'LOT-TUBE-01', 500.000, DATE_ADD(CURDATE(), INTERVAL 24 MONTH), 'DISPONIBLE', p.id_produit
FROM produit p
WHERE p.reference = 'TUBE-SEC'
  AND NOT EXISTS (SELECT 1 FROM lot WHERE numero = 'LOT-TUBE-01' AND produit_id = p.id_produit);

INSERT INTO client (code, raison_sociale, ice, adresse, consentement_cndp, utilisateur_id)
SELECT 'CLI-AMAL', 'Clinique Al Amal', '001888777000019', '45 Rue Ibn Sina, Casablanca', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM client WHERE code = 'CLI-AMAL');

INSERT INTO client (code, raison_sociale, ice, adresse, consentement_cndp, utilisateur_id)
SELECT 'CLI-HOP', 'Hôpital Ibn Rochd', '001111222000008', 'Quartier des Hôpitaux, Casablanca', 1, NULL
WHERE NOT EXISTS (SELECT 1 FROM client WHERE code = 'CLI-HOP');

INSERT INTO patient (code, nom, prenom, date_naissance, sexe, cin, telephone, email, adresse)
SELECT 'PAT-001', 'Benjelloun', 'Amina', '1988-04-12', 'F', 'BE123456', '+212661000001', 'amina.benjelloun@example.ma', 'Casablanca'
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE code = 'PAT-001');

INSERT INTO patient (code, nom, prenom, date_naissance, sexe, cin, telephone, email, adresse)
SELECT 'PAT-002', 'Tazi', 'Omar', '1975-11-03', 'M', 'TA654321', '+212661000002', 'omar.tazi@example.ma', 'Rabat'
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE code = 'PAT-002');

INSERT INTO demande (numero, titre, objectif, date_soumission, statut, client_id, patient_id)
SELECT 'DEM-2026-0001', 'Bilan biochimique Amina Benjelloun', 'Contrôle annuel', NOW(), 'TERMINEE',
       NULL, p.id_patient
FROM patient p
WHERE p.code = 'PAT-001'
  AND NOT EXISTS (SELECT 1 FROM demande WHERE numero = 'DEM-2026-0001');

INSERT INTO demande (numero, titre, objectif, date_soumission, statut, client_id, patient_id)
SELECT 'DEM-2026-0002', 'Analyses Clinique Al Amal', 'Lot patients hospitalisés', NOW(), 'EN_COURS',
       c.id_client, NULL
FROM client c
WHERE c.code = 'CLI-AMAL'
  AND NOT EXISTS (SELECT 1 FROM demande WHERE numero = 'DEM-2026-0002');

INSERT INTO demande (numero, titre, objectif, date_soumission, statut, client_id, patient_id)
SELECT 'DEM-2026-0003', 'Contrôle pH eaux usines', 'Suivi environnemental', NOW(), 'SOUMISE',
       c.id_client, NULL
FROM client c
WHERE c.code = 'CLI-HOP'
  AND NOT EXISTS (SELECT 1 FROM demande WHERE numero = 'DEM-2026-0003');

INSERT INTO echantillon (reference, nature, date_reception, conformite, conditions_conservation, demande_id)
SELECT 'ECH-0001', 'Sang', NOW(), 1, '2-8 °C', d.id_demande
FROM demande d
WHERE d.numero = 'DEM-2026-0001'
  AND NOT EXISTS (SELECT 1 FROM echantillon WHERE reference = 'ECH-0001');

INSERT INTO echantillon (reference, nature, date_reception, conformite, conditions_conservation, demande_id)
SELECT 'ECH-0002', 'Sang', NOW(), 1, '2-8 °C', d.id_demande
FROM demande d
WHERE d.numero = 'DEM-2026-0002'
  AND NOT EXISTS (SELECT 1 FROM echantillon WHERE reference = 'ECH-0002');

INSERT INTO echantillon (reference, nature, date_reception, conformite, conditions_conservation, demande_id)
SELECT 'ECH-0003', 'Eau', NOW(), 1, 'Température ambiante', d.id_demande
FROM demande d
WHERE d.numero = 'DEM-2026-0003'
  AND NOT EXISTS (SELECT 1 FROM echantillon WHERE reference = 'ECH-0003');

INSERT INTO ligne_essai (code, statut, date_attribution, date_debut, date_fin, duree_minutes, valeur, date_saisie, conformite, montant, demande_id, essai_id, echantillon_id)
SELECT 'LE-0001', 'TERMINE', NOW(), NOW(), NOW(), 55, '0.92', NOW(), 1, 45.00,
       d.id_demande, e.id_essai, ech.id_echantillon
FROM demande d
JOIN essai e ON e.code = 'GLY'
JOIN echantillon ech ON ech.reference = 'ECH-0001'
WHERE d.numero = 'DEM-2026-0001'
  AND NOT EXISTS (SELECT 1 FROM ligne_essai WHERE code = 'LE-0001');

INSERT INTO ligne_essai (code, statut, date_attribution, date_debut, valeur, date_saisie, conformite, montant, demande_id, essai_id, echantillon_id)
SELECT 'LE-0002', 'EN_COURS', NOW(), NOW(), '2.10', NOW(), 1, 55.00,
       d.id_demande, e.id_essai, ech.id_echantillon
FROM demande d
JOIN essai e ON e.code = 'CHOL'
JOIN echantillon ech ON ech.reference = 'ECH-0002'
WHERE d.numero = 'DEM-2026-0002'
  AND NOT EXISTS (SELECT 1 FROM ligne_essai WHERE code = 'LE-0002');

INSERT INTO ligne_essai (code, statut, date_attribution, montant, demande_id, essai_id, echantillon_id)
SELECT 'LE-0003', 'PLANIFIE', NOW(), 35.00,
       d.id_demande, e.id_essai, ech.id_echantillon
FROM demande d
JOIN essai e ON e.code = 'EAU-PH'
JOIN echantillon ech ON ech.reference = 'ECH-0003'
WHERE d.numero = 'DEM-2026-0003'
  AND NOT EXISTS (SELECT 1 FROM ligne_essai WHERE code = 'LE-0003');

INSERT INTO consommation_lot (ligne_essai_id, lot_id, quantite_consommee)
SELECT le.id_ligne_essai, l.id_lot, 2.000
FROM ligne_essai le
JOIN lot l ON l.numero = 'LOT-GLU-01'
WHERE le.code = 'LE-0001'
  AND NOT EXISTS (
      SELECT 1 FROM consommation_lot cl
      WHERE cl.ligne_essai_id = le.id_ligne_essai AND cl.lot_id = l.id_lot
  );

INSERT INTO validation (code, niveau, decision, motif, date_validation, signature, ligne_essai_id, validateur_id)
SELECT 'VAL-0001', 2, 'VALIDEE', 'Résultat dans les seuils', NOW(), 'seed-responsable', le.id_ligne_essai, 1
FROM ligne_essai le
WHERE le.code = 'LE-0001'
  AND NOT EXISTS (SELECT 1 FROM validation WHERE code = 'VAL-0001');

INSERT INTO rapport (numero, type, version, date_emission, statut, date_signature, diffuse, demande_id, signataire_id)
SELECT 'RAP-2026-0001', 'FINAL', 1, NOW(), 'SIGNE', NOW(), 1, d.id_demande, 1
FROM demande d
WHERE d.numero = 'DEM-2026-0001'
  AND NOT EXISTS (SELECT 1 FROM rapport WHERE numero = 'RAP-2026-0001');

INSERT INTO facture (numero, type, date_facture, date_echeance, montant_ht, statut, demande_id)
SELECT 'FAC-2026-0001', 'FACTURE', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 30 DAY), 45.00, 'PAYEE', d.id_demande
FROM demande d
WHERE d.numero = 'DEM-2026-0001'
  AND NOT EXISTS (SELECT 1 FROM facture WHERE numero = 'FAC-2026-0001');

INSERT INTO facture_ligne (facture_id, ligne_essai_id, designation, montant)
SELECT f.id_facture, le.id_ligne_essai, 'Glycémie à jeun', 45.00
FROM facture f
JOIN ligne_essai le ON le.code = 'LE-0001'
WHERE f.numero = 'FAC-2026-0001'
  AND NOT EXISTS (
      SELECT 1 FROM facture_ligne fl
      WHERE fl.facture_id = f.id_facture AND fl.ligne_essai_id = le.id_ligne_essai
  );

INSERT INTO paiement (code, date_paiement, montant, mode, reference, facture_id)
SELECT 'PAY-0001', NOW(), 45.00, 'VIREMENT', 'VIR-SEED-001', f.id_facture
FROM facture f
WHERE f.numero = 'FAC-2026-0001'
  AND NOT EXISTS (SELECT 1 FROM paiement WHERE code = 'PAY-0001');

INSERT INTO enquete_satisfaction (code, date_envoi, date_reponse, note_globale, note_delai, note_clarte, note_relation, commentaire, relance_envoyee, demande_id)
SELECT 'ENQ-0001', NOW(), NOW(), 5, 5, 4, 5, 'Prestation conforme.', 0, d.id_demande
FROM demande d
WHERE d.numero = 'DEM-2026-0001'
  AND NOT EXISTS (SELECT 1 FROM enquete_satisfaction WHERE code = 'ENQ-0001');

INSERT INTO evenement_metier (code, type, date_heure, donnees, envoye, nb_tentatives)
SELECT 'EVT-TEN-001', 'DEMANDE_TERMINEE', NOW(), '{"numero":"DEM-2026-0001"}', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM evenement_metier WHERE code = 'EVT-TEN-001');
