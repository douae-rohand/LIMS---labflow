DROP TABLE utilisateur_domaine;

CREATE VIEW utilisateur_domaine AS
SELECT
    CAST(NULL AS UNSIGNED) AS utilisateur_id,
    CAST(NULL AS CHAR(50)) AS code_domaine
WHERE 1 = 0;
