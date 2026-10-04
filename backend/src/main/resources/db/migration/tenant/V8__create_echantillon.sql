CREATE TABLE echantillon (
    id_echantillon BIGINT NOT NULL AUTO_INCREMENT,
    reference VARCHAR(50) NOT NULL,
    nature VARCHAR(100) NULL,
    date_reception TIMESTAMP NULL,
    conformite TINYINT(1) NULL,
    motif TEXT NULL,
    conditions_conservation TEXT NULL,
    demande_id BIGINT NOT NULL,
    receptionneur_id BIGINT NULL,
    PRIMARY KEY (id_echantillon),
    UNIQUE KEY uk_echantillon_reference (reference),
    CONSTRAINT fk_echantillon_demande
        FOREIGN KEY (demande_id) REFERENCES demande (id_demande)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
