CREATE TABLE demande_integration (
    id_demande_integ BIGINT NOT NULL AUTO_INCREMENT,
    numero VARCHAR(50) NOT NULL,
    date_demande TIMESTAMP NOT NULL,
    statut VARCHAR(50) NOT NULL,
    motif_refus TEXT NULL,
    raison_sociale VARCHAR(255) NOT NULL,
    ice VARCHAR(50) NULL,
    adresse TEXT NULL,
    contact_nom VARCHAR(255) NOT NULL,
    contact_email VARCHAR(255) NOT NULL,
    contact_telephone VARCHAR(50) NULL,
    laboratoire_id BIGINT NULL,
    PRIMARY KEY (id_demande_integ),
    UNIQUE KEY uk_demande_integration_numero (numero),
    CONSTRAINT fk_demande_integration_laboratoire
        FOREIGN KEY (laboratoire_id) REFERENCES laboratoire (id_laboratoire)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
