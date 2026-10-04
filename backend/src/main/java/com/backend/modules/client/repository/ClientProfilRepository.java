package com.backend.modules.client.repository;

import com.backend.modules.client.entity.ClientProfil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientProfilRepository extends JpaRepository<ClientProfil, Long> {

    Optional<ClientProfil> findByUtilisateur_Id(Long utilisateurId);

    boolean existsByUtilisateur_Id(Long utilisateurId);
}
