package com.backend.modules.auth.service;

import com.backend.common.exception.BusinessRuleException;
import com.backend.config.JwtConfig;
import com.backend.modules.auth.entity.RefreshToken;
import com.backend.modules.auth.repository.RefreshTokenRepository;
import com.backend.modules.auth.security.JwtTokenProvider;
import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;

/**
 * Gestion du cycle de vie des refresh tokens (persistance, rotation, révocation).
 *
 * <p>La valeur brute du token (JWT signé) n'est jamais stockée.
 * Seul son hash SHA-256 (encodé Base64) est persisté — ce qui empêche
 * toute utilisation en cas de compromission de la base de données.
 *
 * <p>La rotation est atomique : on révoque l'ancien token et on crée le nouveau
 * dans la même transaction. Si la transaction échoue, aucun token n'est modifié.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtConfig jwtConfig;

    // -------------------------------------------------------------------------
    // Persistance
    // -------------------------------------------------------------------------

    /**
     * Persiste un nouveau refresh token pour l'utilisateur donné.
     *
     * @param rawToken  valeur JWT brute retournée au client
     * @param utilisateur  utilisateur propriétaire
     * @param userAgent valeur de l'en-tête User-Agent (peut être null)
     */
    @Transactional
    public void persister(String rawToken, Utilisateur utilisateur, String userAgent) {
        String hash = hash(rawToken);
        Instant expiry = jwtTokenProvider.getExpirationFromToken(rawToken).toInstant();
        RefreshToken entity = RefreshToken.builder()
                .tokenHash(hash)
                .utilisateur(utilisateur)
                .dateExpiration(expiry)
                .userAgent(userAgent != null ? userAgent.substring(0, Math.min(userAgent.length(), 255)) : null)
                .build();
        refreshTokenRepository.save(entity);
        log.debug("Refresh token persisté pour l'utilisateur id={}", utilisateur.getId());
    }

    // -------------------------------------------------------------------------
    // Rotation
    // -------------------------------------------------------------------------

    /**
     * Vérifie le refresh token entrant (type, signature, expiration, DB) ;
     * révoque l'ancien et génère + persiste un nouveau token.
     *
     * @param rawToken  valeur JWT brute envoyée par le client
     * @param userAgent User-Agent de la requête (pour la traçabilité)
     * @return nouveau token JWT brut (à retourner au client)
     * @throws BusinessRuleException si le token est invalide, expiré, révoqué ou introuvable
     */
    @Transactional(noRollbackFor = BusinessRuleException.class)
    public RotationResult rotation(String rawToken, String userAgent) {
        // 1. Vérification cryptographique + type
        if (!jwtTokenProvider.validateToken(rawToken, "refresh")) {
            throw new BusinessRuleException("REFRESH_TOKEN_INVALID", "Refresh token invalide ou expiré");
        }

        // 2. Vérification en base
        String hash = hash(rawToken);
        RefreshToken ancien = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BusinessRuleException("REFRESH_TOKEN_UNKNOWN", "Refresh token inconnu"));

        if (ancien.isRevoque()) {
            // Détection de réutilisation : on révoque tout le compte par sécurité
            log.warn("Réutilisation détectée du refresh token pour userId={} — révocation globale", ancien.getUtilisateur().getId());
            refreshTokenRepository.revoquerTousParUtilisateur(ancien.getUtilisateur().getId());
            throw new BusinessRuleException("REFRESH_TOKEN_REUSE", "Token déjà révoqué — veuillez vous reconnecter");
        }

        if (Instant.now().isAfter(ancien.getDateExpiration())) {
            refreshTokenRepository.revoquerParHash(hash);
            throw new BusinessRuleException("REFRESH_TOKEN_EXPIRED", "Refresh token expiré");
        }

        // 3. Révocation atomique de l'ancien (rotation)
        int affectedRows = refreshTokenRepository.revoquerParHash(hash);
        if (affectedRows == 0) {
            log.warn("Tentative de rotation d'un token déjà révoqué en concurrence pour userId={}", ancien.getUtilisateur().getId());
            refreshTokenRepository.revoquerTousParUtilisateur(ancien.getUtilisateur().getId());
            throw new BusinessRuleException("REFRESH_TOKEN_REUSE", "Token déjà révoqué — veuillez vous reconnecter");
        }

        // 4. Génération + persistance du nouveau
        Utilisateur utilisateur = ancien.getUtilisateur();
        String nouveauToken = jwtTokenProvider.generateRefreshToken(utilisateur.getEmail());
        persister(nouveauToken, utilisateur, userAgent);

        log.debug("Rotation du refresh token pour userId={}", utilisateur.getId());
        return new RotationResult(nouveauToken, utilisateur.getEmail());
    }

    // -------------------------------------------------------------------------
    // Révocation (logout)
    // -------------------------------------------------------------------------

    /**
     * Révoque un token précis (logout simple depuis un seul appareil),
     * en vérifiant qu'il appartient bien à l'utilisateur authentifié.
     */
    @Transactional
    public void revoquer(String rawToken, Long utilisateurId) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        try {
            String hash = hash(rawToken);
            refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
                if (token.getUtilisateur() != null && token.getUtilisateur().getId().equals(utilisateurId)) {
                    int nb = refreshTokenRepository.revoquerParHash(hash);
                    log.debug("Logout : {} token révoqué pour userId={}", nb, utilisateurId);
                } else {
                    log.warn("Tentative de logout avec un refresh token n'appartenant pas à userId={}", utilisateurId);
                }
            });
        } catch (Exception ex) {
            log.warn("Erreur lors de la révocation du token (ignorée) : {}", ex.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Utilitaires
    // -------------------------------------------------------------------------

    /** SHA-256 encodé Base64-URL du token brut. */
    public static String hash(String rawToken) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 non disponible", e);
        }
    }

    // -------------------------------------------------------------------------
    // DTO interne
    // -------------------------------------------------------------------------

    public record RotationResult(String nouveauToken, String email) {}
}
