CREATE TABLE laboratoire (
    id_laboratoire BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL,
    raison_sociale VARCHAR(255) NOT NULL,
    ice VARCHAR(50) NULL,
    adresse TEXT NULL,
    ville VARCHAR(100) NULL,
    telephone VARCHAR(50) NULL,
    email VARCHAR(255) NULL,
    nom_schema VARCHAR(64) NOT NULL,
    statut VARCHAR(50) NOT NULL DEFAULT 'ACTIF',
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_laboratoire),
    UNIQUE KEY uk_laboratoire_code (code),
    UNIQUE KEY uk_laboratoire_nom_schema (nom_schema)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
