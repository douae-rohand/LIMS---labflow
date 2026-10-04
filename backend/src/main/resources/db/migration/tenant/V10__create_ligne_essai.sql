CREATE TABLE ligne_essai (
    id_ligne_essai BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL,
    statut VARCHAR(50) NULL,
    date_attribution TIMESTAMP NULL,
    date_debut TIMESTAMP NULL,
    date_fin TIMESTAMP NULL,
    duree_minutes INT NULL,
    valeur VARCHAR(255) NULL,
    date_saisie TIMESTAMP NULL,
    conformite TINYINT(1) NULL,
    score_anomalie DECIMAL(5,2) NULL,
    montant DECIMAL(12,2) NOT NULL,
    motif TEXT NULL,
    demande_id BIGINT NOT NULL,
    essai_id BIGINT NOT NULL,
    echantillon_id BIGINT NULL,
    technicien_id BIGINT NULL,
    PRIMARY KEY (id_ligne_essai),
    UNIQUE KEY uk_ligne_essai_code (code),
    CONSTRAINT fk_ligne_essai_demande
        FOREIGN KEY (demande_id) REFERENCES demande (id_demande),
    CONSTRAINT fk_ligne_essai_essai
        FOREIGN KEY (essai_id) REFERENCES essai (id_essai),
    CONSTRAINT fk_ligne_essai_echantillon
        FOREIGN KEY (echantillon_id) REFERENCES echantillon (id_echantillon)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
