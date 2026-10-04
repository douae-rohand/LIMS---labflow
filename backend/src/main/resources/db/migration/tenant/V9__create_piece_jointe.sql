CREATE TABLE piece_jointe (
    id_piece BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL,
    nom_fichier VARCHAR(255) NULL,
    type_mime VARCHAR(100) NULL,
    taille BIGINT NULL,
    cle_objet VARCHAR(500) NULL,
    date_ajout TIMESTAMP NULL,
    demande_id BIGINT NOT NULL,
    PRIMARY KEY (id_piece),
    UNIQUE KEY uk_piece_jointe_code (code),
    CONSTRAINT fk_piece_jointe_demande
        FOREIGN KEY (demande_id) REFERENCES demande (id_demande)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
