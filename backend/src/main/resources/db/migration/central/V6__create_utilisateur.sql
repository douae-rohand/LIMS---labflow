CREATE TABLE utilisateur (
    id_utilisateur BIGINT NOT NULL AUTO_INCREMENT,
    matricule VARCHAR(50) NULL,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NULL,
    email VARCHAR(255) NOT NULL,
    telephone VARCHAR(50) NULL,
    cin VARCHAR(20) NULL,
    mot_de_passe_hash VARCHAR(255) NOT NULL,
    must_change_password TINYINT(1) NOT NULL DEFAULT 0,
    double_authentification TINYINT(1) NOT NULL DEFAULT 0,
    derniere_connexion TIMESTAMP NULL,
    actif TINYINT(1) NOT NULL DEFAULT 1,
    role_id BIGINT NOT NULL,
    laboratoire_id BIGINT NULL,
    PRIMARY KEY (id_utilisateur),
    UNIQUE KEY uk_utilisateur_matricule (matricule),
    UNIQUE KEY uk_utilisateur_email (email),
    CONSTRAINT fk_utilisateur_role
        FOREIGN KEY (role_id) REFERENCES role (id_role),
    CONSTRAINT fk_utilisateur_laboratoire
        FOREIGN KEY (laboratoire_id) REFERENCES laboratoire (id_laboratoire)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
