CREATE TABLE panier (
    id_panier BIGINT NOT NULL AUTO_INCREMENT,
    utilisateur_id BIGINT NOT NULL,
    laboratoire_id BIGINT NOT NULL,
    date_creation DATETIME NOT NULL,
    date_modification DATETIME NOT NULL,
    PRIMARY KEY (id_panier),
    UNIQUE KEY uk_panier_utilisateur_laboratoire (utilisateur_id, laboratoire_id),
    KEY idx_panier_utilisateur (utilisateur_id),
    KEY idx_panier_laboratoire (laboratoire_id),
    CONSTRAINT fk_panier_utilisateur
        FOREIGN KEY (utilisateur_id) REFERENCES utilisateur (id_utilisateur),
    CONSTRAINT fk_panier_laboratoire
        FOREIGN KEY (laboratoire_id) REFERENCES laboratoire (id_laboratoire)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ligne_panier (
    id_ligne_panier BIGINT NOT NULL AUTO_INCREMENT,
    panier_id BIGINT NOT NULL,
    essai_id BIGINT NOT NULL,
    date_ajout DATETIME NOT NULL,
    PRIMARY KEY (id_ligne_panier),
    UNIQUE KEY uk_ligne_panier_essai (panier_id, essai_id),
    KEY idx_ligne_panier_panier (panier_id),
    CONSTRAINT fk_ligne_panier_panier
        FOREIGN KEY (panier_id) REFERENCES panier (id_panier) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
