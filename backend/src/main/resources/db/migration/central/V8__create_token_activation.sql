CREATE TABLE token_activation (
    id_token_activation BIGINT NOT NULL AUTO_INCREMENT,
    token_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_expiration DATETIME NOT NULL,
    utilise TINYINT(1) NOT NULL DEFAULT 0,
    utilisateur_id BIGINT NOT NULL,
    PRIMARY KEY (id_token_activation),
    UNIQUE KEY uk_token_activation_token_hash (token_hash),
    CONSTRAINT fk_token_activation_utilisateur
        FOREIGN KEY (utilisateur_id) REFERENCES utilisateur (id_utilisateur)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
