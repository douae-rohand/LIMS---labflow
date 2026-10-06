package com.backend.modules.auth.repository;

import com.backend.modules.auth.entity.TokenActivation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface TokenActivationRepository extends JpaRepository<TokenActivation, Long> {

    Optional<TokenActivation> findByTokenHash(String tokenHash);

    Optional<TokenActivation> findByTokenHashAndUtiliseFalse(String tokenHash);

    List<TokenActivation> findByUtilisateur_IdAndUtiliseFalse(Long utilisateurId);

    /**
     * Marque le jeton comme utilisé de façon atomique, uniquement s'il est
     * encore valide (non utilisé ET non expiré).
     *
     * @return nombre de lignes affectées — 1 si succès, 0 si invalide/expiré/déjà utilisé
     */
    @Modifying
    @Query("""
            UPDATE TokenActivation t
               SET t.utilise = true
             WHERE t.tokenHash = :hash
               AND t.utilise   = false
               AND t.dateExpiration > :now
            """)
    int consommerToken(@Param("hash") String hash, @Param("now") Instant now);

    /** Révoque tous les jetons non encore utilisés d'un utilisateur donné. */
    @Modifying
    @Query("""
            UPDATE TokenActivation t
               SET t.utilise = true
             WHERE t.utilisateur.id = :userId
               AND t.utilise = false
            """)
    int revoquerTousParUtilisateur(@Param("userId") Long userId);
}
