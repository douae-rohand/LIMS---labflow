CREATE TABLE produit (
    id_produit BIGINT NOT NULL AUTO_INCREMENT,
    reference VARCHAR(50) NOT NULL,
    nom VARCHAR(255) NOT NULL,
    unite VARCHAR(50) NOT NULL,
    seuil_minimal DECIMAL(10,3) NULL DEFAULT 0,
    conditions_stockage TEXT NULL,
    PRIMARY KEY (id_produit),
    UNIQUE KEY uk_produit_reference (reference)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
