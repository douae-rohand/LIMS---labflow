CREATE TABLE facture_ligne (
    id_ligne BIGINT NOT NULL AUTO_INCREMENT,
    facture_id BIGINT NOT NULL,
    ligne_essai_id BIGINT NULL,
    designation VARCHAR(255) NOT NULL,
    montant DECIMAL(12,2) NOT NULL,
    PRIMARY KEY (id_ligne),
    CONSTRAINT fk_facture_ligne_facture
        FOREIGN KEY (facture_id) REFERENCES facture (id_facture),
    CONSTRAINT fk_facture_ligne_ligne_essai
        FOREIGN KEY (ligne_essai_id) REFERENCES ligne_essai (id_ligne_essai)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
