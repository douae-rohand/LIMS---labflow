CREATE TABLE domaine (
    id_domaine BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL,
    libelle VARCHAR(100) NOT NULL,
    PRIMARY KEY (id_domaine),
    UNIQUE KEY uk_domaine_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
