package com.backend.modules.auth.service;

/**
 * Port d'envoi du lien d'activation par email.
 *
 * <p>Implémentation active : {@link ActivationEmailServiceImpl} (tous les profils).
 * En profil {@code dev}, le lien est aussi journalisé pour faciliter les tests.
 *
 * @param email     adresse email du destinataire
 * @param nomComplet nom complet de l'utilisateur (prénom + nom)
 * @param tokenBrut valeur brute du jeton (32 octets en Base64-URL)
 */
public interface ActivationEmailService {
    void envoyerLienActivation(String email, String nomComplet, String tokenBrut);
}
