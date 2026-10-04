CREATE TABLE notification (
    id_notification BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL,
    titre VARCHAR(255) NOT NULL,
    date_creation TIMESTAMP NOT NULL,
    PRIMARY KEY (id_notification),
    UNIQUE KEY uk_notification_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
