package com.backend.config;

import com.backend.modules.utilisateur.entity.Role;
import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.repository.RoleRepository;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Initialiseur idempotent du compte Super Administrateur initial.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SuperAdminInitializer implements CommandLineRunner {

    @Value("${SUPER_ADMIN_EMAIL:admin@lims.com}")
    private String superAdminEmail;

    @Value("${SUPER_ADMIN_PASSWORD:change-me}")
    private String superAdminPassword;

    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (!StringUtils.hasText(superAdminPassword) || "change-me".equalsIgnoreCase(superAdminPassword.trim())) {
            log.warn("SUPER_ADMIN_PASSWORD est vide ou vaut 'change-me' : création du compte Super Administrateur ignorée.");
            return;
        }

        if (!StringUtils.hasText(superAdminEmail)) {
            log.warn("SUPER_ADMIN_EMAIL n'est pas renseigné : création du compte Super Administrateur ignorée.");
            return;
        }

        if (utilisateurRepository.existsByEmail(superAdminEmail)) {
            log.info("Le compte Super Administrateur ({}) existe déjà.", superAdminEmail);
            return;
        }

        Role roleSuperAdmin = roleRepository.findByCode("SUPER_ADMINISTRATEUR")
                .orElseThrow(() -> new IllegalStateException("Le rôle 'SUPER_ADMINISTRATEUR' est introuvable en base. Seeding V15 incomplet."));

        Utilisateur superAdmin = Utilisateur.builder()
                .matricule("SA-001")
                .nom("Super")
                .prenom("Admin")
                .email(superAdminEmail)
                .motDePasseHash(passwordEncoder.encode(superAdminPassword))
                .role(roleSuperAdmin)
                .laboratoire(null)
                .actif(true)
                .doubleAuthentification(false)
                .mustChangePassword(true) // Forced password change on first login
                .build();

        utilisateurRepository.save(superAdmin);
        log.info("Compte Super Administrateur créé avec succès pour l'email : {}", superAdminEmail);
    }
}
