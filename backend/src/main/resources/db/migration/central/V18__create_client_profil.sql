CREATE TABLE client_profil (
    id_client_profil BIGINT NOT NULL AUTO_INCREMENT,
    utilisateur_id BIGINT NOT NULL,
    raison_sociale VARCHAR(255) NOT NULL,
    ice VARCHAR(50) NULL,
    adresse TEXT NULL,
    consentement_cndp TINYINT(1) NOT NULL DEFAULT 0,
    date_creation DATETIME NOT NULL,
    PRIMARY KEY (id_client_profil),
    UNIQUE KEY uk_client_profil_utilisateur (utilisateur_id),
    CONSTRAINT fk_client_profil_utilisateur
        FOREIGN KEY (utilisateur_id) REFERENCES utilisateur (id_utilisateur)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
