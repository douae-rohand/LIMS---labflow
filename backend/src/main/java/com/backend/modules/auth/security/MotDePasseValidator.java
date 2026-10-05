package com.backend.modules.auth.security;

import com.backend.common.exception.BusinessRuleException;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Validateur de politique de mot de passe réutilisable.
 *
 * <p>Utilisé par :
 * <ul>
 *   <li>Changement de mot de passe (M01)</li>
 *   <li>Activation de compte (lot C)</li>
 *   <li>Réinitialisation de mot de passe (lot C)</li>
 * </ul>
 *
 * <p>Politique :
 * <ul>
 *   <li>Au moins 10 caractères</li>
 *   <li>Au moins une lettre minuscule</li>
 *   <li>Au moins une lettre majuscule</li>
 *   <li>Au moins un chiffre</li>
 *   <li>Différent du mot de passe actuel (comparaison BCrypt)</li>
 * </ul>
 */
public final class MotDePasseValidator {

    private MotDePasseValidator() {}

    /**
     * Valide le nouveau mot de passe par rapport à la politique et à l'ancien hash.
     *
     * @param nouveauMotDePasse  mot de passe en clair proposé
     * @param ancienHash         hash BCrypt du mot de passe actuel (pour vérifier qu'ils sont différents)
     * @param passwordEncoder    encodeur BCrypt pour la comparaison
     * @throws BusinessRuleException (400) si une règle est violée
     */
    public static void valider(String nouveauMotDePasse, String ancienHash,
                               PasswordEncoder passwordEncoder) {
        if (nouveauMotDePasse == null || nouveauMotDePasse.length() < 10) {
            throw new BusinessRuleException("MOT_DE_PASSE_TROP_COURT",
                    "Le nouveau mot de passe doit contenir au moins 10 caractères.");
        }
        if (!nouveauMotDePasse.chars().anyMatch(Character::isLowerCase)) {
            throw new BusinessRuleException("MOT_DE_PASSE_SANS_MINUSCULE",
                    "Le nouveau mot de passe doit contenir au moins une lettre minuscule.");
        }
        if (!nouveauMotDePasse.chars().anyMatch(Character::isUpperCase)) {
            throw new BusinessRuleException("MOT_DE_PASSE_SANS_MAJUSCULE",
                    "Le nouveau mot de passe doit contenir au moins une lettre majuscule.");
        }
        if (!nouveauMotDePasse.chars().anyMatch(Character::isDigit)) {
            throw new BusinessRuleException("MOT_DE_PASSE_SANS_CHIFFRE",
                    "Le nouveau mot de passe doit contenir au moins un chiffre.");
        }
        if (ancienHash != null && passwordEncoder.matches(nouveauMotDePasse, ancienHash)) {
            throw new BusinessRuleException("MOT_DE_PASSE_IDENTIQUE",
                    "Le nouveau mot de passe doit être différent du mot de passe actuel.");
        }
    }
}
