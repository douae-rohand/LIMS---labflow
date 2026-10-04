CREATE TABLE consommation_lot (
    ligne_essai_id BIGINT NOT NULL,
    lot_id BIGINT NOT NULL,
    quantite_consommee DECIMAL(10,3) NOT NULL,
    PRIMARY KEY (ligne_essai_id, lot_id),
    CONSTRAINT fk_consommation_lot_ligne_essai
        FOREIGN KEY (ligne_essai_id) REFERENCES ligne_essai (id_ligne_essai),
    CONSTRAINT fk_consommation_lot_lot
        FOREIGN KEY (lot_id) REFERENCES lot (id_lot)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
