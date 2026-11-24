-- V35 (tenant) compare code_domaine à domaine.code (utf8mb4_unicode_ci). Sans collation explicite, la vue
-- de V40 prend celle de la connexion (utf8mb4_general_ci) et la création d'un laboratoire échoue.
CREATE OR REPLACE VIEW utilisateur_domaine AS
SELECT
    CAST(NULL AS UNSIGNED) AS utilisateur_id,
    CONVERT(CAST(NULL AS CHAR(50)) USING utf8mb4) COLLATE utf8mb4_unicode_ci AS code_domaine
WHERE 1 = 0;
