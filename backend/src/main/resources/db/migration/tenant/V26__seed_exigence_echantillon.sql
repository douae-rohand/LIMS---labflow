-- Données de démo fournies explicitement, pas déduites des désignations.
-- Colonne du code d'essai : essai.code (V4__create_essai.sql).

INSERT INTO exigence_echantillon (essai_id, nature, etat_conservation, quantite_minimale, unite)
SELECT e.id_essai, 'Sang', '2 à 8 °C, à jeun', NULL, NULL
FROM essai e
WHERE e.code = 'GLY'
  AND NOT EXISTS (
      SELECT 1 FROM exigence_echantillon x
      WHERE x.essai_id = e.id_essai AND x.nature = 'Sang'
  );

INSERT INTO exigence_echantillon (essai_id, nature, etat_conservation, quantite_minimale, unite)
SELECT e.id_essai, 'Sang', '2 à 8 °C', NULL, NULL
FROM essai e
WHERE e.code = 'CHOL'
  AND NOT EXISTS (
      SELECT 1 FROM exigence_echantillon x
      WHERE x.essai_id = e.id_essai AND x.nature = 'Sang'
  );

INSERT INTO exigence_echantillon (essai_id, nature, etat_conservation, quantite_minimale, unite)
SELECT e.id_essai, 'Sang', '2 à 8 °C', NULL, NULL
FROM essai e
WHERE e.code = 'NFS'
  AND NOT EXISTS (
      SELECT 1 FROM exigence_echantillon x
      WHERE x.essai_id = e.id_essai AND x.nature = 'Sang'
  );

INSERT INTO exigence_echantillon (essai_id, nature, etat_conservation, quantite_minimale, unite)
SELECT e.id_essai, 'Selles', 'Température ambiante', NULL, NULL
FROM essai e
WHERE e.code = 'CULT'
  AND NOT EXISTS (
      SELECT 1 FROM exigence_echantillon x
      WHERE x.essai_id = e.id_essai AND x.nature = 'Selles'
  );

INSERT INTO exigence_echantillon (essai_id, nature, etat_conservation, quantite_minimale, unite)
SELECT e.id_essai, 'Eau', 'Température ambiante', NULL, NULL
FROM essai e
WHERE e.code = 'EAU-PH'
  AND NOT EXISTS (
      SELECT 1 FROM exigence_echantillon x
      WHERE x.essai_id = e.id_essai AND x.nature = 'Eau'
  );
