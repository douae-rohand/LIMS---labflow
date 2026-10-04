CREATE TABLE enquete_satisfaction (
    id_enquete BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL,
    date_envoi TIMESTAMP NULL,
    date_reponse TIMESTAMP NULL,
    note_globale INT NULL,
    note_delai INT NULL,
    note_clarte INT NULL,
    note_relation INT NULL,
    commentaire TEXT NULL,
    relance_envoyee TINYINT(1) NULL DEFAULT 0,
    demande_id BIGINT NOT NULL,
    PRIMARY KEY (id_enquete),
    UNIQUE KEY uk_enquete_satisfaction_code (code),
    CONSTRAINT fk_enquete_satisfaction_demande
        FOREIGN KEY (demande_id) REFERENCES demande (id_demande)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
