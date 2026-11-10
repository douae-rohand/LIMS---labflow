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

    /** Politique de base : 10 caractères, minuscule, majuscule et chiffre. */
    public static void valider(String nouveauMotDePasse, String ancienHash,
                               PasswordEncoder passwordEncoder) {
        valider(nouveauMotDePasse, ancienHash, passwordEncoder, 10, false);
    }

    /**
     * Valide le nouveau mot de passe selon la politique de la plateforme.
     *
     * @param longueurMin     longueur minimale (jamais moins de 10)
     * @param exigerSpecial   exige au moins un caractère qui n'est ni une lettre ni un chiffre
     * @throws BusinessRuleException (400) si une règle est violée
     */
    public static void valider(String nouveauMotDePasse, String ancienHash,
                               PasswordEncoder passwordEncoder, int longueurMin, boolean exigerSpecial) {
        int minimum = Math.max(10, longueurMin);
        if (nouveauMotDePasse == null || nouveauMotDePasse.length() < minimum) {
            throw new BusinessRuleException("MOT_DE_PASSE_TROP_COURT",
                    "Le nouveau mot de passe doit contenir au moins " + minimum + " caractères.");
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
        if (exigerSpecial && nouveauMotDePasse.chars().allMatch(Character::isLetterOrDigit)) {
            throw new BusinessRuleException("MOT_DE_PASSE_SANS_SPECIAL",
                    "Le nouveau mot de passe doit contenir au moins un caractère spécial.");
        }
        if (ancienHash != null && passwordEncoder.matches(nouveauMotDePasse, ancienHash)) {
            throw new BusinessRuleException("MOT_DE_PASSE_IDENTIQUE",
                    "Le nouveau mot de passe doit être différent du mot de passe actuel.");
        }
    }
}
