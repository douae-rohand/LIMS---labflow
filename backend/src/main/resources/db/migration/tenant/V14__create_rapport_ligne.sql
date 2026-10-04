CREATE TABLE rapport_ligne (
    rapport_id BIGINT NOT NULL,
    ligne_essai_id BIGINT NOT NULL,
    PRIMARY KEY (rapport_id, ligne_essai_id),
    CONSTRAINT fk_rapport_ligne_rapport
        FOREIGN KEY (rapport_id) REFERENCES rapport (id_rapport),
    CONSTRAINT fk_rapport_ligne_ligne_essai
        FOREIGN KEY (ligne_essai_id) REFERENCES ligne_essai (id_ligne_essai)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
