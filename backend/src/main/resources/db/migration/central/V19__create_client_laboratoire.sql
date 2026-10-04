CREATE TABLE client_laboratoire (
    utilisateur_id BIGINT NOT NULL,
    laboratoire_id BIGINT NOT NULL,
    client_local_id BIGINT NOT NULL,
    statut VARCHAR(20) NOT NULL DEFAULT 'ACTIF',
    date_premier_contact DATETIME NOT NULL,
    PRIMARY KEY (utilisateur_id, laboratoire_id),
    KEY idx_client_laboratoire_laboratoire (laboratoire_id),
    CONSTRAINT fk_client_lab_utilisateur
        FOREIGN KEY (utilisateur_id) REFERENCES utilisateur (id_utilisateur),
    CONSTRAINT fk_client_lab_laboratoire
        FOREIGN KEY (laboratoire_id) REFERENCES laboratoire (id_laboratoire)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
