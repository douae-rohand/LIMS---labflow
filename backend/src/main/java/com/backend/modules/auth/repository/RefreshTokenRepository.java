package com.backend.modules.auth.repository;

import com.backend.modules.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Révoque tous les tokens non-expirés d'un utilisateur (logout global ou compromission). */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoque = true WHERE rt.utilisateur.id = :userId AND rt.revoque = false")
    int revoquerTousParUtilisateur(@Param("userId") Long userId);

    /** Révoque un token précis identifié par son hash s'il n'est pas déjà révoqué. */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoque = true WHERE rt.tokenHash = :hash AND rt.revoque = false")
    int revoquerParHash(@Param("hash") String hash);
}
