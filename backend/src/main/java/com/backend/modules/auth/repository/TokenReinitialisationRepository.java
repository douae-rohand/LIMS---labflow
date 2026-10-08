package com.backend.modules.auth.repository;

import com.backend.modules.auth.entity.TokenReinitialisation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface TokenReinitialisationRepository extends JpaRepository<TokenReinitialisation, Long> {

    Optional<TokenReinitialisation> findByTokenHash(String tokenHash);

    /**
     * Marque le jeton comme utilisé de façon atomique, uniquement s'il est
     * valide (utilise = false ET date_expiration > now).
     *
     * @return 1 si le jeton a été consommé, 0 si invalide / expiré / déjà utilisé
     */
    @Modifying
    @Query("""
            UPDATE TokenReinitialisation t
               SET t.utilise = true
             WHERE t.tokenHash       = :hash
               AND t.utilise         = false
               AND t.dateExpiration  > :now
            """)
    int consommerToken(@Param("hash") String hash, @Param("now") Instant now);

    /**
     * Révoque tous les jetons non utilisés d'un utilisateur
     * (lors d'une nouvelle demande, les jetons précédents sont invalidés).
     */
    @Modifying
    @Query("""
            UPDATE TokenReinitialisation t
               SET t.utilise = true
             WHERE t.utilisateur.id = :userId
               AND t.utilise        = false
            """)
    int revoquerTousParUtilisateur(@Param("userId") Long userId);
}
