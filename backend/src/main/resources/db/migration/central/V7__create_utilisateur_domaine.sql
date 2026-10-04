CREATE TABLE utilisateur_domaine (
    utilisateur_id BIGINT NOT NULL,
    code_domaine VARCHAR(50) NOT NULL,
    PRIMARY KEY (utilisateur_id, code_domaine)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
