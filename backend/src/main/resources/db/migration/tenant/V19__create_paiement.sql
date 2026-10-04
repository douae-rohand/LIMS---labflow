CREATE TABLE paiement (
    id_paiement BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL,
    date_paiement TIMESTAMP NOT NULL,
    montant DECIMAL(12,2) NOT NULL,
    mode VARCHAR(50) NULL,
    reference VARCHAR(255) NULL,
    facture_id BIGINT NOT NULL,
    encaisseur_id BIGINT NULL,
    PRIMARY KEY (id_paiement),
    UNIQUE KEY uk_paiement_code (code),
    CONSTRAINT fk_paiement_facture
        FOREIGN KEY (facture_id) REFERENCES facture (id_facture)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
