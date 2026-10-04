CREATE TABLE permission (
    id_permission BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(80) NOT NULL,
    module VARCHAR(100) NULL,
    action VARCHAR(100) NULL,
    PRIMARY KEY (id_permission),
    UNIQUE KEY uk_permission_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
