CREATE TABLE evenement_metier (
    id_evenement BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL,
    `type` VARCHAR(80) NOT NULL,
    date_heure TIMESTAMP NOT NULL,
    donnees JSON NOT NULL,
    envoye TINYINT(1) NOT NULL DEFAULT 0,
    nb_tentatives INT NOT NULL DEFAULT 0,
    derniere_erreur TEXT NULL,
    laboratoire_id BIGINT NULL,
    PRIMARY KEY (id_evenement),
    UNIQUE KEY uk_evenement_metier_code (code),
    CONSTRAINT fk_evenement_metier_laboratoire
        FOREIGN KEY (laboratoire_id) REFERENCES laboratoire (id_laboratoire)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
