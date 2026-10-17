package com.backend.modules.panier.repository;

import com.backend.modules.panier.entity.LignePanier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface LignePanierRepository extends JpaRepository<LignePanier, Long> {

    List<LignePanier> findByPanierId(Long panierId);

    List<LignePanier> findByPanierIdIn(Collection<Long> panierIds);

    Optional<LignePanier> findByPanierIdAndEssaiId(Long panierId, Long essaiId);

    void deleteByPanierIdAndEssaiId(Long panierId, Long essaiId);

    long countByPanierId(Long panierId);
}
