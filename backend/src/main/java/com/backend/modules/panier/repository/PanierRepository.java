package com.backend.modules.panier.repository;

import com.backend.modules.panier.entity.Panier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PanierRepository extends JpaRepository<Panier, Long> {

    Optional<Panier> findByUtilisateurIdAndLaboratoireId(Long utilisateurId, Long laboratoireId);

    List<Panier> findByUtilisateurId(Long utilisateurId);
}
