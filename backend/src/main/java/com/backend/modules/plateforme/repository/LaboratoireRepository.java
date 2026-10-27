package com.backend.modules.plateforme.repository;

import com.backend.modules.plateforme.entity.Laboratoire;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LaboratoireRepository extends JpaRepository<Laboratoire, Long> {

    Optional<Laboratoire> findByCode(String code);

    Optional<Laboratoire> findByNomSchema(String nomSchema);

    Optional<Laboratoire> findByIdAndStatut(Long id, String statut);

    boolean existsByCode(String code);

    boolean existsByIce(String ice);

    boolean existsByIceAndIdNot(String ice, Long id);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    long countByStatut(String statut);

    @Query("""
            SELECT DISTINCT l.ville FROM Laboratoire l
            WHERE l.ville IS NOT NULL AND l.ville <> ''
            ORDER BY l.ville
            """)
    List<String> findVillesDistinctes();

    @Query("""
            SELECT l FROM Laboratoire l
            WHERE (:statut IS NULL OR l.statut = :statut)
              AND (:ville IS NULL OR LOWER(l.ville) = LOWER(:ville))
              AND (
                    :recherche IS NULL
                    OR LOWER(l.raisonSociale) LIKE LOWER(CONCAT('%', :recherche, '%')) ESCAPE '\\'
                    OR LOWER(l.code) LIKE LOWER(CONCAT('%', :recherche, '%')) ESCAPE '\\'
                    OR LOWER(COALESCE(l.ville, '')) LIKE LOWER(CONCAT('%', :recherche, '%')) ESCAPE '\\'
                    OR LOWER(COALESCE(l.email, '')) LIKE LOWER(CONCAT('%', :recherche, '%')) ESCAPE '\\'
                  )
            """)
    Page<Laboratoire> rechercher(@Param("recherche") String recherche,
                                 @Param("statut") String statut,
                                 @Param("ville") String ville,
                                 Pageable pageable);

    @Query("""
            SELECT l FROM Laboratoire l
            WHERE (:statut IS NULL OR l.statut = :statut)
              AND (:ville IS NULL OR LOWER(l.ville) = LOWER(:ville))
              AND (
                    :recherche IS NULL
                    OR LOWER(l.raisonSociale) LIKE LOWER(CONCAT('%', :recherche, '%')) ESCAPE '\\'
                    OR LOWER(l.code) LIKE LOWER(CONCAT('%', :recherche, '%')) ESCAPE '\\'
                    OR LOWER(COALESCE(l.ville, '')) LIKE LOWER(CONCAT('%', :recherche, '%')) ESCAPE '\\'
                    OR LOWER(COALESCE(l.email, '')) LIKE LOWER(CONCAT('%', :recherche, '%')) ESCAPE '\\'
                  )
            """)
    List<Laboratoire> rechercherListe(@Param("recherche") String recherche,
                                      @Param("statut") String statut,
                                      @Param("ville") String ville,
                                      Sort sort);

    Page<Laboratoire> findByStatut(String statut, Pageable pageable);

    java.util.List<Laboratoire> findByStatutOrderByRaisonSocialeAsc(String statut);
}
