package com.backend.modules.auth.repository;

import com.backend.modules.auth.entity.TokenActivation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TokenActivationRepository extends JpaRepository<TokenActivation, Long> {

    Optional<TokenActivation> findByTokenHashAndUtiliseFalse(String tokenHash);

    List<TokenActivation> findByUtilisateur_IdAndUtiliseFalse(Long utilisateurId);
}
