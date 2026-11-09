package com.backend.modules.essai.repository;

import com.backend.modules.essai.entity.LigneEssai;
import com.backend.modules.essai.entity.StatutEssai;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface LigneEssaiRepository extends JpaRepository<LigneEssai, Long> {

    List<LigneEssai> findByEchantillon_Id(Long echantillonId);

    List<LigneEssai> findByDemande_Id(Long demandeId);

    /**
     * Charge les lignes de plusieurs demandes avec leur essai et le domaine
     * de l'essai (LEFT JOIN FETCH). Ne jamais appeler avec une collection vide.
     */
    @Query("""
            SELECT le FROM LigneEssai le
            LEFT JOIN FETCH le.essai e
            LEFT JOIN FETCH e.domaine
            WHERE le.demande.id IN :demandeIds
            """)
    List<LigneEssai> findByDemande_IdIn(@Param("demandeIds") Collection<Long> demandeIds);

    Page<LigneEssai> findByTechnicienId(Long technicienId, Pageable pageable);

    /** Lignes des demandes dans les statuts donnés, avec demande, essai et domaine. */
    @Query("""
            SELECT le FROM LigneEssai le
            JOIN FETCH le.demande d
            LEFT JOIN FETCH le.essai e
            LEFT JOIN FETCH e.domaine
            WHERE d.statut IN :statuts
            ORDER BY d.dateSoumission ASC, le.id ASC
            """)
    List<LigneEssai> findLignesDesDemandes(@Param("statuts") Collection<String> statuts);

    @Query("""
            SELECT le FROM LigneEssai le
            JOIN FETCH le.demande d
            LEFT JOIN FETCH le.essai e
            LEFT JOIN FETCH e.domaine
            WHERE le.id IN :ids
            """)
    List<LigneEssai> findAvecDetails(@Param("ids") Collection<Long> ids);

    Page<LigneEssai> findByStatut(StatutEssai statut, Pageable pageable);

    boolean existsByEssaiId(Long essaiId);
}
