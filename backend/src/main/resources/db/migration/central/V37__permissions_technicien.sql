-- ============================================================
-- V37 : Nouvelles permissions Espace Technicien (CMD-13)
--
-- VALIDATION_TECHNIQUE_GERER : validation niveau 1 (technicien)
--   Séparée de VALIDATION_GERER (niveau 2 Responsable) afin qu'un
--   technicien ne puisse pas accéder aux endpoints de validation
--   responsable. Délégable = 1 (l'administrateur peut la retirer).
--
-- ESSAIS_IMPORT_CSV : import groupé de résultats par CSV.
--   Délégable = 1.
--
-- Défaut : TECHNICIEN uniquement (pas ADMINISTRATEUR : il n'agit
--   pas directement sur les essais).
--
-- Idempotent : INSERT IGNORE protège contre une double application.
-- La colonne `deleguable` a été ajoutée en V36 (information_schema).
-- ============================================================

INSERT IGNORE INTO permission (code, module, action, libelle, deleguable)
VALUES
    ('VALIDATION_TECHNIQUE_GERER', 'VALIDATION_TECHNIQUE', 'GERER',
     'Soumettre la validation technique (niveau 1)', 1),
    ('ESSAIS_IMPORT_CSV', 'ESSAIS', 'IMPORT_CSV',
     'Importer des résultats d''essai par CSV', 1);

-- Rattacher au rôle TECHNICIEN (idempotent via NOT EXISTS)
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id_role, p.id_permission
FROM role r
         JOIN permission p ON p.code IN ('VALIDATION_TECHNIQUE_GERER', 'ESSAIS_IMPORT_CSV')
WHERE r.code = 'TECHNICIEN'
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp
    WHERE rp.role_id = r.id_role AND rp.permission_id = p.id_permission
);
