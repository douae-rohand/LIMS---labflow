-- ============================================================
-- V37 : deux statuts de demande entre l'acceptation et le démarrage
--   ECHANTILLONS_RECUS : chaque analyse réalisable a un échantillon conforme
--   PLANIFIEE          : chaque analyse réalisable est en plus affectée à un technicien
-- EN_COURS ne se pose plus à l'affectation mais au premier « Démarrer » du technicien.
-- Reclassement des demandes existantes qui n'ont encore aucune analyse démarrée.
-- ============================================================

-- Demandes « avant démarrage » : acceptées, ou EN_COURS sans aucune analyse démarrée
-- (anciennement passées EN_COURS dès l'affectation).
CREATE TEMPORARY TABLE tmp_avancement AS
SELECT d.id_demande,
       SUM(CASE WHEN le.statut <> 'NON_REALISABLE' OR le.statut IS NULL THEN 1 ELSE 0 END) AS realisables,
       SUM(CASE WHEN (le.statut <> 'NON_REALISABLE' OR le.statut IS NULL)
                 AND e.conformite = 1 THEN 1 ELSE 0 END) AS recues,
       SUM(CASE WHEN (le.statut <> 'NON_REALISABLE' OR le.statut IS NULL)
                 AND e.conformite = 1 AND le.technicien_id IS NOT NULL THEN 1 ELSE 0 END) AS affectees
FROM demande d
JOIN ligne_essai le ON le.demande_id = d.id_demande
LEFT JOIN echantillon e ON e.id_echantillon = le.echantillon_id
WHERE d.statut IN ('ACCEPTEE', 'EN_COURS')
  AND NOT EXISTS (SELECT 1 FROM ligne_essai x
                  WHERE x.demande_id = d.id_demande AND x.statut IN ('EN_COURS', 'TERMINE'))
GROUP BY d.id_demande;

UPDATE demande d
JOIN tmp_avancement t ON t.id_demande = d.id_demande
SET d.statut = CASE
        WHEN t.realisables > 0 AND t.affectees = t.realisables THEN 'PLANIFIEE'
        WHEN t.realisables > 0 AND t.recues = t.realisables THEN 'ECHANTILLONS_RECUS'
        ELSE 'ACCEPTEE'
    END;

DROP TEMPORARY TABLE tmp_avancement;
