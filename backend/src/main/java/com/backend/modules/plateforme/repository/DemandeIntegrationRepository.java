package com.backend.modules.plateforme.repository;

import com.backend.modules.plateforme.entity.DemandeIntegration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DemandeIntegrationRepository extends JpaRepository<DemandeIntegration, Long> {

    Page<DemandeIntegration> findByStatut(String statut, Pageable pageable);

    boolean existsByContactEmailAndStatut(String contactEmail, String statut);

    boolean existsByIceAndStatut(String ice, String statut);

    long countByStatut(String statut);

    java.util.Optional<DemandeIntegration> findFirstByLaboratoire_IdOrderByIdDesc(Long laboratoireId);

    @Query("""
            SELECT d FROM DemandeIntegration d
            WHERE (:statut IS NULL OR d.statut = :statut)
              AND (
                    :recherche IS NULL
                    OR LOWER(COALESCE(d.nomLaboratoire, '')) LIKE LOWER(CONCAT('%', :recherche, '%')) ESCAPE '\\'
                    OR LOWER(d.raisonSociale) LIKE LOWER(CONCAT('%', :recherche, '%')) ESCAPE '\\'
                    OR LOWER(d.contactEmail) LIKE LOWER(CONCAT('%', :recherche, '%')) ESCAPE '\\'
                    OR LOWER(d.numero) LIKE LOWER(CONCAT('%', :recherche, '%')) ESCAPE '\\'
                    OR LOWER(COALESCE(d.ville, '')) LIKE LOWER(CONCAT('%', :recherche, '%')) ESCAPE '\\'
                  )
            """)
    Page<DemandeIntegration> rechercher(@Param("statut") String statut,
                                        @Param("recherche") String recherche,
                                        Pageable pageable);
}
