CREATE TABLE IF NOT EXISTS exigence_echantillon (
    id_exigence BIGINT NOT NULL AUTO_INCREMENT,
    essai_id BIGINT NOT NULL,
    nature VARCHAR(100) NOT NULL,
    etat_conservation VARCHAR(255) NOT NULL,
    quantite_minimale DECIMAL(10,2) NULL,
    unite VARCHAR(20) NULL,
    PRIMARY KEY (id_exigence),
    UNIQUE KEY uk_exigence_essai_nature (essai_id, nature),
    KEY idx_exigence_echantillon_essai (essai_id),
    CONSTRAINT fk_exigence_echantillon_essai
        FOREIGN KEY (essai_id) REFERENCES essai (id_essai) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
