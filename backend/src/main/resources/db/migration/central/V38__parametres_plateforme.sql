-- ============================================================
-- V38 : Paramètres globaux de la plateforme (Super Administrateur)
--   * paramètres clé/valeur (sécurité)
--   * documents exigés pour une demande d'intégration
--   * référentiels : types de laboratoire, domaines d'analyse
--   * demande de compléments sur une demande d'intégration
--   * notifications : contenu et date de lecture
-- ============================================================

CREATE TABLE parametre_plateforme (
    cle VARCHAR(100) NOT NULL,
    valeur VARCHAR(1000) NOT NULL,
    date_modification TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modifie_par VARCHAR(255) NULL,
    PRIMARY KEY (cle)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO parametre_plateforme (cle, valeur) VALUES
    ('securite.2fa.roles', ''),
    ('securite.mdp.longueur_min', '10'),
    ('securite.mdp.exiger_special', 'false'),
    ('securite.session.duree_jours', '7');

CREATE TABLE type_document_integration (
    code VARCHAR(80) NOT NULL,
    libelle VARCHAR(255) NOT NULL,
    raison VARCHAR(255) NULL,
    obligatoire TINYINT(1) NOT NULL DEFAULT 1,
    actif TINYINT(1) NOT NULL DEFAULT 1,
    ordre INT NOT NULL DEFAULT 0,
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO type_document_integration (code, libelle, raison, obligatoire, actif, ordre) VALUES
    ('AUTORISATION_OUVERTURE_EXPLOITATION', 'Autorisation d''ouverture et d''exploitation du laboratoire', 'Vérifier que le laboratoire est autorisé', 1, 1, 1),
    ('DIPLOME_BIOLOGISTE_RESPONSABLE', 'Diplôme / titre du biologiste responsable', 'Vérifier la qualification du responsable', 1, 1, 2),
    ('INSCRIPTION_ORDRE_PROFESSIONNEL', 'Document d''inscription à l''ordre professionnel', 'Vérifier son statut professionnel', 1, 1, 3),
    ('CIN_BIOLOGISTE_RESPONSABLE', 'CIN du biologiste responsable', 'Vérifier son identité', 1, 1, 4),
    ('IDENTIFICATION_JURIDIQUE', 'Registre / document d''identification juridique du laboratoire', 'Vérifier l''existence juridique', 1, 1, 5),
    ('JUSTIFICATIF_ADRESSE', 'Justificatif de l''adresse du laboratoire', 'Vérifier l''adresse déclarée', 1, 1, 6);

CREATE TABLE referentiel_type_laboratoire (
    id BIGINT NOT NULL AUTO_INCREMENT,
    libelle VARCHAR(100) NOT NULL,
    actif TINYINT(1) NOT NULL DEFAULT 1,
    ordre INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_referentiel_type_laboratoire_libelle (libelle)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO referentiel_type_laboratoire (libelle, actif, ordre) VALUES
    ('Médical', 1, 1),
    ('Industriel', 1, 2),
    ('Environnemental', 1, 3),
    ('Pharmaceutique et cosmétique', 1, 4);

CREATE TABLE referentiel_domaine (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL,
    libelle VARCHAR(100) NOT NULL,
    actif TINYINT(1) NOT NULL DEFAULT 1,
    ordre INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_referentiel_domaine_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO referentiel_domaine (code, libelle, actif, ordre) VALUES
    ('BIOCHIMIE', 'Biochimie', 1, 1),
    ('HEMATOLOGIE', 'Hématologie', 1, 2),
    ('MICROBIOLOGIE', 'Microbiologie', 1, 3),
    ('ENVIRONNEMENT', 'Environnement', 1, 4);

ALTER TABLE demande_integration
    ADD COLUMN complements_demandes TEXT NULL,
    ADD COLUMN documents_complements VARCHAR(1000) NULL,
    ADD COLUMN date_demande_complements TIMESTAMP NULL,
    ADD COLUMN date_reception_complements TIMESTAMP NULL,
    ADD COLUMN jeton_complements_hash VARCHAR(100) NULL,
    ADD COLUMN jeton_complements_expiration TIMESTAMP NULL,
    ADD KEY idx_demande_integration_jeton (jeton_complements_hash);

ALTER TABLE notification
    ADD COLUMN corps TEXT NULL,
    ADD COLUMN lien VARCHAR(500) NULL,
    ADD COLUMN type_notification VARCHAR(50) NULL;

ALTER TABLE notification_reception
    ADD COLUMN date_lecture TIMESTAMP NULL;
