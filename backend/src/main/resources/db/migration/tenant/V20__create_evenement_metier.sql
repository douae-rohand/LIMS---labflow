CREATE TABLE evenement_metier (
    id_evenement BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL,
    `type` VARCHAR(80) NOT NULL,
    date_heure TIMESTAMP NOT NULL,
    donnees JSON NOT NULL,
    envoye TINYINT(1) NULL DEFAULT 0,
    nb_tentatives INT NULL DEFAULT 0,
    derniere_erreur TEXT NULL,
    PRIMARY KEY (id_evenement),
    UNIQUE KEY uk_evenement_metier_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
