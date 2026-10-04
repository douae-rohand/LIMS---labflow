CREATE TABLE synthese_ia (
    id_synthese BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL,
    contenu TEXT NOT NULL,
    date_generation TIMESTAMP NOT NULL,
    demande_id BIGINT NOT NULL,
    PRIMARY KEY (id_synthese),
    UNIQUE KEY uk_synthese_ia_code (code),
    CONSTRAINT fk_synthese_ia_demande
        FOREIGN KEY (demande_id) REFERENCES demande (id_demande)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
