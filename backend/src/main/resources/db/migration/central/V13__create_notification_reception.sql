CREATE TABLE notification_reception (
    notification_id BIGINT NOT NULL,
    utilisateur_id BIGINT NOT NULL,
    canal VARCHAR(30) NULL,
    statut_envoi VARCHAR(30) NULL,
    lue TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (notification_id, utilisateur_id),
    CONSTRAINT fk_notification_reception_notification
        FOREIGN KEY (notification_id) REFERENCES notification (id_notification),
    CONSTRAINT fk_notification_reception_utilisateur
        FOREIGN KEY (utilisateur_id) REFERENCES utilisateur (id_utilisateur)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
