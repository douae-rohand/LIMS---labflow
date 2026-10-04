CREATE TABLE rapport (
    id_rapport BIGINT NOT NULL AUTO_INCREMENT,
    numero VARCHAR(50) NOT NULL,
    `type` VARCHAR(50) NULL,
    version SMALLINT NULL DEFAULT 1,
    date_emission TIMESTAMP NULL,
    statut VARCHAR(50) NULL,
    date_signature TIMESTAMP NULL,
    cle_pdf VARCHAR(500) NULL,
    diffuse TINYINT(1) NULL DEFAULT 0,
    demande_id BIGINT NOT NULL,
    signataire_id BIGINT NULL,
    PRIMARY KEY (id_rapport),
    UNIQUE KEY uk_rapport_numero (numero),
    CONSTRAINT fk_rapport_demande
        FOREIGN KEY (demande_id) REFERENCES demande (id_demande)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
