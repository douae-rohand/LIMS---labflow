CREATE TABLE lot (
    id_lot BIGINT NOT NULL AUTO_INCREMENT,
    numero VARCHAR(50) NOT NULL,
    quantite DECIMAL(10,3) NOT NULL,
    peremption DATE NOT NULL,
    statut VARCHAR(50) NOT NULL,
    produit_id BIGINT NULL,
    PRIMARY KEY (id_lot),
    UNIQUE KEY uk_lot_produit_numero (produit_id, numero),
    CONSTRAINT fk_lot_produit
        FOREIGN KEY (produit_id) REFERENCES produit (id_produit)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
