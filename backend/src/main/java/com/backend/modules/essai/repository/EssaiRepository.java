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

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    /**
     * Recherche dans le catalogue du schéma courant.
     * {@code filtrerActif} à false ignore {@code actif} et renvoie tout le catalogue du laboratoire.
     */
    @Query(
            value = """
                    SELECT e FROM Essai e
                    LEFT JOIN e.domaine d
                    WHERE (:filtrerActif = false OR e.actif = :actif)
                      AND (
                        :q = ''
                        OR LOWER(e.code) LIKE LOWER(CONCAT('%', :q, '%'))
                        OR LOWER(e.designation) LIKE LOWER(CONCAT('%', :q, '%'))
                        OR LOWER(COALESCE(e.methode, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                        OR LOWER(COALESCE(d.libelle, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                        OR LOWER(COALESCE(d.code, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                      )
                    """,
            countQuery = """
                    SELECT COUNT(e) FROM Essai e
                    LEFT JOIN e.domaine d
                    WHERE (:filtrerActif = false OR e.actif = :actif)
                      AND (
                        :q = ''
                        OR LOWER(e.code) LIKE LOWER(CONCAT('%', :q, '%'))
                        OR LOWER(e.designation) LIKE LOWER(CONCAT('%', :q, '%'))
                        OR LOWER(COALESCE(e.methode, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                        OR LOWER(COALESCE(d.libelle, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                        OR LOWER(COALESCE(d.code, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                      )
                    """)
    Page<Essai> rechercher(
            @Param("q") String q,
            @Param("filtrerActif") boolean filtrerActif,
            @Param("actif") boolean actif,
            Pageable pageable);

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
