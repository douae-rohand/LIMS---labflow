CREATE TABLE patient (
    id_patient BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    date_naissance DATE NULL,
    sexe VARCHAR(10) NULL,
    cin VARCHAR(20) NULL,
    telephone VARCHAR(50) NULL,
    email VARCHAR(255) NULL,
    adresse TEXT NULL,
    PRIMARY KEY (id_patient),
    UNIQUE KEY uk_patient_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
