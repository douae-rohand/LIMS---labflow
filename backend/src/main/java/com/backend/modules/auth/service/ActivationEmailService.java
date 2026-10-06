package com.backend.modules.auth.service;

/**
 * Port d'envoi du lien d'activation par email.
 *
 * <p>Implémentations :
 * <ul>
 *   <li>{@link DevActivationEmailService} — profil {@code dev} : log uniquement.</li>
 *   <li>Un bean réel (JavaMailSender / SendGrid) à créer pour le profil {@code prod}.</li>
 * </ul>
 *
 * @param email     adresse email du destinataire
 * @param tokenBrut valeur brute du jeton (32 octets en Base64-URL)
 */
public interface ActivationEmailService {
    void envoyerLienActivation(String email, String tokenBrut);
}
