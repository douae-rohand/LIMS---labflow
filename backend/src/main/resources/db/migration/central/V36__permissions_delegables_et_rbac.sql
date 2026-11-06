-- Keep the schema column and permission seeds safe to apply only once.
SET @permission_deleguable_sql = IF(
    (SELECT COUNT(*)
     FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'permission'
       AND COLUMN_NAME = 'deleguable') = 0,
    'ALTER TABLE permission ADD COLUMN deleguable TINYINT(1) NOT NULL DEFAULT 1',
    'SELECT 1'
);
PREPARE permission_deleguable_stmt FROM @permission_deleguable_sql;
EXECUTE permission_deleguable_stmt;
DEALLOCATE PREPARE permission_deleguable_stmt;

INSERT INTO permission (code, module, action, libelle)
SELECT seed.code, seed.module, seed.action, seed.libelle
FROM (
    SELECT 'FACTURATION_PAYER' AS code, 'FACTURATION' AS module, 'PAYER' AS action, 'Enregistrer un paiement' AS libelle
    UNION ALL SELECT 'PARAMETRES_LABORATOIRE_GERER', 'PARAMETRES_LABORATOIRE', 'GERER', 'Gérer les paramètres du laboratoire'
    UNION ALL SELECT 'AUDIT_CONSULTER', 'AUDIT', 'CONSULTER', 'Consulter le journal d’audit'
    UNION ALL SELECT 'INDICATEURS_CONSULTER', 'INDICATEURS', 'CONSULTER', 'Consulter les indicateurs'
    UNION ALL SELECT 'EXPORT_DEMANDES', 'EXPORT', 'DEMANDES', 'Exporter les demandes'
    UNION ALL SELECT 'EXPORT_ENQUETES', 'EXPORT', 'ENQUETES', 'Exporter les enquêtes de satisfaction'
    UNION ALL SELECT 'EXPORT_FACTURES', 'EXPORT', 'FACTURES', 'Exporter les factures'
    UNION ALL SELECT 'EXPORT_PRODUITS', 'EXPORT', 'PRODUITS', 'Exporter les produits'
    UNION ALL SELECT 'EXPORT_EMPLOYES', 'EXPORT', 'EMPLOYES', 'Exporter les employés'
    UNION ALL SELECT 'EXPORT_AUDIT', 'EXPORT', 'AUDIT', 'Exporter le journal d’audit'
) AS seed
LEFT JOIN permission existing ON existing.code = seed.code
WHERE existing.id_permission IS NULL;

UPDATE permission
SET deleguable = CASE
    WHEN code IN (
        'EMPLOYES_CONSULTER', 'EMPLOYES_GERER',
        'CATALOGUE_CONSULTER', 'CATALOGUE_GERER',
        'FACTURATION_CONSULTER', 'FACTURATION_GERER', 'FACTURATION_PAYER',
        'STOCK_GERER', 'SATISFACTION_CONSULTER', 'NOTIFICATIONS_GERER',
        'PARAMETRES_LABORATOIRE_GERER', 'AUDIT_CONSULTER', 'INDICATEURS_CONSULTER',
        'EXPORT_FACTURES', 'EXPORT_AUDIT', 'EXPORT_EMPLOYES'
    ) THEN 0
    ELSE 1
END;

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id_role, p.id_permission
FROM (
    SELECT 'ADMINISTRATEUR' AS role_code, 'FACTURATION_PAYER' AS permission_code
    UNION ALL SELECT 'ADMINISTRATEUR', 'PARAMETRES_LABORATOIRE_GERER'
    UNION ALL SELECT 'ADMINISTRATEUR', 'AUDIT_CONSULTER'
    UNION ALL SELECT 'ADMINISTRATEUR', 'INDICATEURS_CONSULTER'
    UNION ALL SELECT 'ADMINISTRATEUR', 'EXPORT_DEMANDES'
    UNION ALL SELECT 'ADMINISTRATEUR', 'EXPORT_ENQUETES'
    UNION ALL SELECT 'ADMINISTRATEUR', 'EXPORT_FACTURES'
    UNION ALL SELECT 'ADMINISTRATEUR', 'EXPORT_PRODUITS'
    UNION ALL SELECT 'ADMINISTRATEUR', 'EXPORT_EMPLOYES'
    UNION ALL SELECT 'ADMINISTRATEUR', 'EXPORT_AUDIT'
    UNION ALL SELECT 'ACCUEIL', 'FACTURATION_PAYER'
) AS seed
JOIN role r ON r.code = seed.role_code
JOIN permission p ON p.code = seed.permission_code
LEFT JOIN role_permission existing
    ON existing.role_id = r.id_role AND existing.permission_id = p.id_permission
WHERE existing.role_id IS NULL;

DELETE role_permission
FROM role_permission
JOIN role ON role.id_role = role_permission.role_id
JOIN permission ON permission.id_permission = role_permission.permission_id
WHERE permission.deleguable = 0
  AND role.code <> 'ADMINISTRATEUR'
  AND NOT (role.code = 'ACCUEIL' AND permission.code = 'FACTURATION_PAYER');

DELETE utilisateur_permission
FROM utilisateur_permission
JOIN permission ON permission.id_permission = utilisateur_permission.permission_id
WHERE permission.deleguable = 0;
