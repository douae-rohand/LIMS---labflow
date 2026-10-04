CREATE TABLE journal_audit (
    id_audit BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL,
    date_action TIMESTAMP NOT NULL,
    action VARCHAR(100) NULL,
    objet VARCHAR(100) NULL,
    valeur_avant JSON NULL,
    valeur_apres JSON NULL,
    motif TEXT NULL,
    utilisateur_id BIGINT NOT NULL,
    PRIMARY KEY (id_audit),
    UNIQUE KEY uk_journal_audit_code (code),
    CONSTRAINT fk_journal_audit_utilisateur
        FOREIGN KEY (utilisateur_id) REFERENCES utilisateur (id_utilisateur)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
