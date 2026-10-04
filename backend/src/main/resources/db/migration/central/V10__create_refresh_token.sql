CREATE TABLE refresh_token (
    id_refresh_token BIGINT NOT NULL AUTO_INCREMENT,
    token_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_expiration TIMESTAMP NOT NULL,
    revoque TINYINT(1) NOT NULL DEFAULT 0,
    user_agent VARCHAR(255) NULL,
    utilisateur_id BIGINT NOT NULL,
    PRIMARY KEY (id_refresh_token),
    UNIQUE KEY uk_refresh_token_token_hash (token_hash),
    CONSTRAINT fk_refresh_token_utilisateur
        FOREIGN KEY (utilisateur_id) REFERENCES utilisateur (id_utilisateur)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
