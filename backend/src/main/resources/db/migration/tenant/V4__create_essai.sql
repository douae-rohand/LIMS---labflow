CREATE TABLE essai (
    id_essai BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL,
    designation VARCHAR(255) NOT NULL,
    description TEXT NULL,
    methode VARCHAR(255) NULL,
    tarif DECIMAL(12,2) NULL DEFAULT 0,
    duree_estimee INT NOT NULL,
    unite VARCHAR(50) NULL,
    limite_min DECIMAL(12,4) NULL,
    limite_max DECIMAL(12,4) NULL,
    seuil_critique_min DECIMAL(12,4) NULL,
    seuil_critique_max DECIMAL(12,4) NULL,
    actif TINYINT(1) NULL DEFAULT 1,
    domaine_id BIGINT NULL,
    PRIMARY KEY (id_essai),
    UNIQUE KEY uk_essai_code (code),
    CONSTRAINT fk_essai_domaine
        FOREIGN KEY (domaine_id) REFERENCES domaine (id_domaine)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
