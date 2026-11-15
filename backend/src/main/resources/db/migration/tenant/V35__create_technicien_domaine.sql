CREATE TABLE technicien_domaine (
    technicien_id BIGINT NOT NULL,
    domaine_id BIGINT NOT NULL,
    PRIMARY KEY (technicien_id, domaine_id),
    CONSTRAINT fk_technicien_domaine_domaine
        FOREIGN KEY (domaine_id) REFERENCES domaine(id_domaine) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO technicien_domaine (technicien_id, domaine_id)
SELECT ud.utilisateur_id, d.id_domaine
FROM lims_central.utilisateur_domaine ud
JOIN lims_central.utilisateur u ON u.id_utilisateur = ud.utilisateur_id
JOIN lims_central.role r ON r.id_role = u.role_id AND r.code = 'TECHNICIEN'
JOIN lims_central.laboratoire l ON l.id_laboratoire = u.laboratoire_id
LEFT JOIN domaine d ON d.code = ud.code_domaine
WHERE l.nom_schema = DATABASE();
