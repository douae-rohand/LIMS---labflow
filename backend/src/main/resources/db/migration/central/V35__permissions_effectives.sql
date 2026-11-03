ALTER TABLE permission
    ADD COLUMN libelle VARCHAR(150) NULL;

INSERT INTO permission (code, module, action, libelle) VALUES
('EMPLOYES_CONSULTER', 'EMPLOYES', 'CONSULTER', 'Consulter les employés'),
('EMPLOYES_GERER', 'EMPLOYES', 'GERER', 'Gérer les employés'),
('DEMANDES_CONSULTER', 'DEMANDES', 'CONSULTER', 'Consulter les demandes du laboratoire'),
('DEMANDES_SUIVI', 'DEMANDES', 'SUIVI', 'Suivre les demandes par statut'),
('DEMANDES_STATUT', 'DEMANDES', 'STATUT', 'Mettre à jour le statut d''une demande'),
('CATALOGUE_CONSULTER', 'CATALOGUE', 'CONSULTER', 'Consulter le catalogue'),
('CATALOGUE_GERER', 'CATALOGUE', 'GERER', 'Gérer le catalogue'),
('ECHANTILLONS_GERER', 'ECHANTILLONS', 'GERER', 'Enregistrer un échantillon'),
('ESSAIS_GERER', 'ESSAIS', 'GERER', 'Réaliser les essais'),
('VALIDATION_GERER', 'VALIDATION', 'GERER', 'Créer une validation'),
('RAPPORTS_GERER', 'RAPPORTS', 'GERER', 'Générer un rapport'),
('FACTURATION_CONSULTER', 'FACTURATION', 'CONSULTER', 'Consulter la facturation'),
('FACTURATION_GERER', 'FACTURATION', 'GERER', 'Gérer la facturation'),
('PLANIFICATION_GERER', 'PLANIFICATION', 'GERER', 'Créer une planification'),
('PLANIFICATION_STATUT', 'PLANIFICATION', 'STATUT', 'Mettre à jour une planification'),
('STOCK_CONSULTER', 'STOCK', 'CONSULTER', 'Consulter le stock'),
('STOCK_GERER', 'STOCK', 'GERER', 'Gérer le stock'),
('CONFORMITE_CONSULTER', 'CONFORMITE', 'CONSULTER', 'Consulter les non-conformités'),
('CONFORMITE_DECLARER', 'CONFORMITE', 'DECLARER', 'Déclarer une non-conformité'),
('CONFORMITE_GERER', 'CONFORMITE', 'GERER', 'Traiter une non-conformité'),
('IA_GERER', 'IA', 'GERER', 'Lancer une analyse assistée'),
('SATISFACTION_CONSULTER', 'SATISFACTION', 'CONSULTER', 'Consulter la satisfaction'),
('NOTIFICATIONS_GERER', 'NOTIFICATIONS', 'GERER', 'Publier une notification');

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id_role, p.id_permission
FROM role r
JOIN permission p ON (
    (r.code = 'ADMINISTRATEUR' AND p.code IN (
        'EMPLOYES_CONSULTER', 'EMPLOYES_GERER',
        'DEMANDES_CONSULTER', 'DEMANDES_SUIVI', 'DEMANDES_STATUT',
        'CATALOGUE_CONSULTER', 'CATALOGUE_GERER',
        'VALIDATION_GERER', 'RAPPORTS_GERER',
        'FACTURATION_CONSULTER', 'FACTURATION_GERER',
        'PLANIFICATION_GERER', 'PLANIFICATION_STATUT',
        'STOCK_CONSULTER', 'STOCK_GERER',
        'CONFORMITE_CONSULTER', 'CONFORMITE_DECLARER', 'CONFORMITE_GERER',
        'IA_GERER', 'SATISFACTION_CONSULTER', 'NOTIFICATIONS_GERER'))
    OR (r.code = 'RESPONSABLE' AND p.code IN (
        'DEMANDES_CONSULTER', 'DEMANDES_SUIVI', 'DEMANDES_STATUT',
        'CATALOGUE_CONSULTER', 'CATALOGUE_GERER',
        'ESSAIS_GERER', 'VALIDATION_GERER', 'RAPPORTS_GERER',
        'FACTURATION_CONSULTER', 'FACTURATION_GERER',
        'PLANIFICATION_GERER', 'PLANIFICATION_STATUT',
        'STOCK_CONSULTER', 'STOCK_GERER',
        'CONFORMITE_CONSULTER', 'CONFORMITE_DECLARER', 'CONFORMITE_GERER',
        'IA_GERER', 'SATISFACTION_CONSULTER', 'NOTIFICATIONS_GERER'))
    OR (r.code = 'TECHNICIEN' AND p.code IN (
        'DEMANDES_SUIVI', 'ESSAIS_GERER', 'PLANIFICATION_STATUT',
        'STOCK_CONSULTER', 'STOCK_GERER',
        'CONFORMITE_CONSULTER', 'CONFORMITE_DECLARER', 'IA_GERER'))
    OR (r.code = 'ACCUEIL' AND p.code IN (
        'DEMANDES_SUIVI', 'DEMANDES_STATUT', 'ECHANTILLONS_GERER', 'FACTURATION_CONSULTER'))
);

CREATE TABLE utilisateur_permission (
    id BIGINT NOT NULL AUTO_INCREMENT,
    utilisateur_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    effet VARCHAR(10) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_utilisateur_permission (utilisateur_id, permission_id),
    KEY idx_utilisateur_permission_utilisateur (utilisateur_id),
    CONSTRAINT fk_utilisateur_permission_utilisateur
        FOREIGN KEY (utilisateur_id) REFERENCES utilisateur (id_utilisateur),
    CONSTRAINT fk_utilisateur_permission_permission
        FOREIGN KEY (permission_id) REFERENCES permission (id_permission)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
