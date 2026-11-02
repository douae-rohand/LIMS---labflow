-- Historique des changements de statut des laboratoires
-- Stocke l'ancien statut, le nouveau statut, le motif, l'acteur et la date
-- Pour l'onglet "Historique" de la fiche laboratoire

CREATE TABLE historique_statut_laboratoire (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    laboratoire_id BIGINT NOT NULL,
    ancien_statut VARCHAR(20),
    nouveau_statut VARCHAR(20) NOT NULL,
    motif TEXT,
    acteur_email VARCHAR(255) NOT NULL,
    date_changement TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_laboratoire (laboratoire_id),
    INDEX idx_date (date_changement),
    CONSTRAINT fk_historique_laboratoire FOREIGN KEY (laboratoire_id) 
        REFERENCES laboratoire(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
