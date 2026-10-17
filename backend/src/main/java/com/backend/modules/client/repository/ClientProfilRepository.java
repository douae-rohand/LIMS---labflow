package com.backend.modules.client.repository;

import com.backend.modules.client.entity.ClientProfil;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientProfilRepository extends JpaRepository<ClientProfil, Long> {

    /**
     * Le profil est lu en central puis réutilisé hors session (ex.
     * {@code ClientService.trouverOuCreerFiche}) : l'utilisateur doit donc être
     * initialisé pendant que la session centrale est ouverte, sinon
     * {@code profil.getUtilisateur().getNomComplet()} lève une
     * {@code LazyInitializationException}.
     */
    @EntityGraph(attributePaths = "utilisateur")
    Optional<ClientProfil> findByUtilisateur_Id(Long utilisateurId);

    boolean existsByUtilisateur_Id(Long utilisateurId);
}
