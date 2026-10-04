CREATE TABLE validation (
    id_validation BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL,
    niveau INT NOT NULL,
    decision VARCHAR(50) NULL,
    motif TEXT NULL,
    date_validation TIMESTAMP NOT NULL,
    signature VARCHAR(255) NULL,
    ligne_essai_id BIGINT NOT NULL,
    validateur_id BIGINT NOT NULL,
    PRIMARY KEY (id_validation),
    UNIQUE KEY uk_validation_code (code),
    CONSTRAINT fk_validation_ligne_essai
        FOREIGN KEY (ligne_essai_id) REFERENCES ligne_essai (id_ligne_essai)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
