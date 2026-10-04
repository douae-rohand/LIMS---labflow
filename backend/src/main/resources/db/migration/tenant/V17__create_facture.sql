CREATE TABLE facture (
    id_facture BIGINT NOT NULL AUTO_INCREMENT,
    numero VARCHAR(50) NOT NULL,
    `type` VARCHAR(50) NULL,
    date_facture DATE NULL,
    date_echeance DATE NULL,
    montant_ht DECIMAL(12,2) NOT NULL,
    statut VARCHAR(50) NULL,
    demande_id BIGINT NOT NULL,
    PRIMARY KEY (id_facture),
    UNIQUE KEY uk_facture_numero (numero),
    CONSTRAINT fk_facture_demande
        FOREIGN KEY (demande_id) REFERENCES demande (id_demande)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
