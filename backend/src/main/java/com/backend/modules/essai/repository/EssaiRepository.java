package com.backend.modules.essai.repository;

import com.backend.modules.essai.entity.Essai;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EssaiRepository extends JpaRepository<Essai, Long> {

    Optional<Essai> findByCode(String code);

    boolean existsByCode(String code);

    Page<Essai> findByActif(Boolean actif, Pageable pageable);

    Optional<Essai> findFirstByActifTrue();

    @Query("SELECT e FROM Essai e LEFT JOIN FETCH e.domaine WHERE e.actif = true ORDER BY e.designation")
    List<Essai> findActifsAvecDomaine();

    /**
     * Charge les essais par identifiants avec leur domaine (LEFT JOIN FETCH).
     * Ne jamais appeler avec une collection vide (génère une SQL invalide).
     */
    @Query("SELECT e FROM Essai e LEFT JOIN FETCH e.domaine WHERE e.id IN :ids")
    List<Essai> findByIdsAvecDomaine(@Param("ids") Collection<Long> ids);
}
