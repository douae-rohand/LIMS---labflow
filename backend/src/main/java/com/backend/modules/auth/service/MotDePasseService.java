package com.backend.modules.auth.service;

import com.backend.common.exception.BusinessRuleException;
import com.backend.modules.auth.dto.ChangerMotDePasseRequest;
import com.backend.modules.auth.repository.RefreshTokenRepository;
import com.backend.modules.auth.security.MotDePasseValidator;
import com.backend.modules.auth.security.UtilisateurPrincipal;
import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Logique métier du changement de mot de passe par l'utilisateur connecté.
 *
 * <p>Séquence :
 * <ol>
 *   <li>Vérification de l'ancien mot de passe (BCrypt) → 400 si incorrect (jamais 401).</li>
 *   <li>Validation de la politique du nouveau mot de passe via {@link MotDePasseValidator}.</li>
 *   <li>Hash BCrypt + sauvegarde + {@code must_change_password = false}.</li>
 *   <li>Révocation de TOUS les refresh tokens de l'utilisateur.</li>
 *   <li>Émission d'une nouvelle paire (accès + refresh) via {@link AuthService#buildLoginResult}.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MotDePasseService {

    private final UtilisateurRepository utilisateurRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    @Transactional
    public AuthService.LoginResult changerMotDePasse(ChangerMotDePasseRequest request,
                                                      UtilisateurPrincipal principal,
                                                      HttpServletRequest httpRequest) {
        Utilisateur utilisateur = utilisateurRepository.findById(principal.getId())
                .orElseThrow(() -> new BusinessRuleException("USER_NOT_FOUND", "Utilisateur introuvable"));

        // 1. Vérification de l'ancien mot de passe — 400, JAMAIS 401
        //    (une 401 déclencherait un refresh côté frontend en boucle)
        if (!passwordEncoder.matches(request.getAncienMotDePasse(), utilisateur.getMotDePasseHash())) {
            throw new BusinessRuleException("ANCIEN_MOT_DE_PASSE_INCORRECT",
                    "Mot de passe actuel incorrect.");
        }

        // 2. Politique du nouveau mot de passe (réutilisable dans lot C)
        MotDePasseValidator.valider(
                request.getNouveauMotDePasse(),
                utilisateur.getMotDePasseHash(),
                passwordEncoder);

        // 3. Mise à jour — on ne logue jamais un mot de passe en clair
        utilisateur.setMotDePasseHash(passwordEncoder.encode(request.getNouveauMotDePasse()));
        utilisateur.setMustChangePassword(false);
        utilisateurRepository.save(utilisateur);
        log.info("Mot de passe changé pour userId={}", utilisateur.getId());

        // 4. Révocation de TOUS les refresh tokens existants (invalidation des sessions ouvertes)
        int nbRevoked = refreshTokenRepository.revoquerTousParUtilisateur(utilisateur.getId());
        log.debug("Révocation de {} refresh token(s) pour userId={}", nbRevoked, utilisateur.getId());

        // 5. Émission d'une nouvelle paire (accès + refresh) — principal rechargé avec mustChangePassword=false
        UtilisateurPrincipal principalMisAJour = UtilisateurPrincipal.builder()
                .id(utilisateur.getId())
                .email(utilisateur.getEmail())
                .password(utilisateur.getMotDePasseHash())
                .nomComplet(utilisateur.getNomComplet())
                .role(principal.getRole())
                .laboratoireId(principal.getLaboratoireId())
                .nomSchema(principal.getNomSchema())
                .actif(utilisateur.isActif())
                .mustChangePassword(false)
                .doubleAuthentification(utilisateur.isDoubleAuthentification())
                .build();

        return authService.buildLoginResult(principalMisAJour, httpRequest);
    }
}
