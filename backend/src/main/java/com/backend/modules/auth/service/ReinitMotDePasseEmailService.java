package com.backend.modules.auth.service;

/**
 * Port d'envoi du lien de réinitialisation de mot de passe.
 *
 * <p>Implémentation active : {@link ReinitMotDePasseEmailServiceImpl} (tous profils).
 * En profil {@code dev}, le lien est aussi journalisé pour faciliter les tests.
 */
public interface ReinitMotDePasseEmailService {
    /**
     * Envoie le lien de réinitialisation à l'utilisateur.
     *
     * @param email      adresse e-mail du destinataire
     * @param nomComplet nom complet de l'utilisateur
     * @param tokenBrut  valeur brute du jeton (jamais loggée sauf en dev)
     */
    void envoyerLienReinit(String email, String nomComplet, String tokenBrut);
}
