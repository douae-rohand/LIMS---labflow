CREATE TABLE demande (
    id_demande BIGINT NOT NULL AUTO_INCREMENT,
    numero VARCHAR(50) NOT NULL,
    titre VARCHAR(255) NOT NULL,
    objectif TEXT NULL,
    date_soumission TIMESTAMP NOT NULL,
    statut VARCHAR(50) NOT NULL,
    date_decision TIMESTAMP NULL,
    motif TEXT NULL,
    client_id BIGINT NULL,
    patient_id BIGINT NULL,
    decideur_id BIGINT NULL,
    PRIMARY KEY (id_demande),
    UNIQUE KEY uk_demande_numero (numero),
    CONSTRAINT fk_demande_client
        FOREIGN KEY (client_id) REFERENCES client (id_client),
    CONSTRAINT fk_demande_patient
        FOREIGN KEY (patient_id) REFERENCES patient (id_patient),
    CONSTRAINT chk_demande_tiers
        CHECK (client_id IS NOT NULL OR patient_id IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
