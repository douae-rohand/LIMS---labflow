CREATE TABLE client (
    id_client BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL,
    raison_sociale VARCHAR(255) NOT NULL,
    ice VARCHAR(50) NULL,
    adresse TEXT NULL,
    consentement_cndp TINYINT(1) NULL DEFAULT 0,
    utilisateur_id BIGINT NULL,
    PRIMARY KEY (id_client),
    UNIQUE KEY uk_client_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
