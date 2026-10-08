-- ============================================================
-- V28 : Table de séquence et génération des matricules utilisateurs
-- ============================================================

CREATE TABLE IF NOT EXISTS lims_central.matricule_sequence (
    prefixe VARCHAR(10) NOT NULL,
    annee INT NOT NULL,
    dernier_numero INT NOT NULL DEFAULT 0,
    PRIMARY KEY (prefixe, annee)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Procedure temporaire de rattrapage des matricules manquants
DROP PROCEDURE IF EXISTS lims_central.sp_rattraper_matricules;

DELIMITER //

CREATE PROCEDURE lims_central.sp_rattraper_matricules()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE u_id BIGINT;
    DECLARE r_code VARCHAR(50);
    DECLARE u_annee INT;
    DECLARE p_prefixe VARCHAR(10);
    DECLARE next_num INT;
    DECLARE m_formatted VARCHAR(50);

    DECLARE cur CURSOR FOR
        SELECT u.id_utilisateur, r.code, YEAR(COALESCE(u.date_creation, NOW()))
        FROM lims_central.utilisateur u
        JOIN lims_central.role r ON u.role_id = r.id_role
        WHERE u.matricule IS NULL
           OR u.matricule = ''
           OR u.matricule NOT REGEXP '^[A-Z]+-[0-9]{4}-[0-9]{6}$'
        ORDER BY u.id_utilisateur ASC;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN cur;

    read_loop: LOOP
        FETCH cur INTO u_id, r_code, u_annee;
        IF done THEN
            LEAVE read_loop;
        END IF;

        CASE r_code
            WHEN 'CLIENT' THEN SET p_prefixe = 'CLI';
            WHEN 'ADMINISTRATEUR' THEN SET p_prefixe = 'ADM';
            WHEN 'RESPONSABLE' THEN SET p_prefixe = 'RES';
            WHEN 'RESPONSABLE_LABO' THEN SET p_prefixe = 'RES';
            WHEN 'TECHNICIEN' THEN SET p_prefixe = 'TEC';
            WHEN 'ACCUEIL' THEN SET p_prefixe = 'ACC';
            WHEN 'SUPER_ADMINISTRATEUR' THEN SET p_prefixe = 'SAD';
            ELSE SET p_prefixe = 'USR';
        END CASE;

        -- Obtenir ou initialiser le compteur
        INSERT INTO lims_central.matricule_sequence (prefixe, annee, dernier_numero)
        VALUES (p_prefixe, u_annee, 1)
        ON DUPLICATE KEY UPDATE dernier_numero = dernier_numero + 1;

        SELECT dernier_numero INTO next_num
        FROM lims_central.matricule_sequence
        WHERE prefixe = p_prefixe AND annee = u_annee;

        SET m_formatted = CONCAT(p_prefixe, '-', u_annee, '-', LPAD(next_num, 6, '0'));

        UPDATE lims_central.utilisateur
        SET matricule = m_formatted
        WHERE id_utilisateur = u_id;

    END LOOP;

    CLOSE cur;
END //

DELIMITER ;

CALL lims_central.sp_rattraper_matricules();
DROP PROCEDURE IF EXISTS lims_central.sp_rattraper_matricules;
