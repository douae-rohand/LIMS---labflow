package com.backend.modules.demande.repository;

import com.backend.modules.demande.entity.Demande;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DemandeRepository extends JpaRepository<Demande, Long> {

    Optional<Demande> findByNumero(String numero);

    Page<Demande> findByClient_Id(Long clientId, Pageable pageable);

    Page<Demande> findByClient_UtilisateurId(Long utilisateurId, Pageable pageable);

    java.util.List<Demande> findByClient_UtilisateurId(Long utilisateurId);

    Page<Demande> findByStatut(String statut, Pageable pageable);

    long countByStatut(String statut);

    /**
     * Recherche les demandes du schéma courant.
     * {@code q} et {@code statut} vides signifient « sans filtre ».
     */
    @Query(
            value = """
                    SELECT d FROM Demande d
                    LEFT JOIN d.client c
                    LEFT JOIN d.patient p
                    WHERE (:statut = '' OR d.statut = :statut)
                      AND (
                            :q = ''
                            OR LOWER(d.numero) LIKE LOWER(CONCAT('%', :q, '%'))
                            OR LOWER(d.titre) LIKE LOWER(CONCAT('%', :q, '%'))
                            OR (c.raisonSociale IS NOT NULL AND LOWER(c.raisonSociale) LIKE LOWER(CONCAT('%', :q, '%')))
                            OR (p.nom IS NOT NULL AND LOWER(p.nom) LIKE LOWER(CONCAT('%', :q, '%')))
                            OR (p.prenom IS NOT NULL AND LOWER(p.prenom) LIKE LOWER(CONCAT('%', :q, '%')))
                      )
                    """,
            countQuery = """
                    SELECT COUNT(d) FROM Demande d
                    LEFT JOIN d.client c
                    LEFT JOIN d.patient p
                    WHERE (:statut = '' OR d.statut = :statut)
                      AND (
                            :q = ''
                            OR LOWER(d.numero) LIKE LOWER(CONCAT('%', :q, '%'))
                            OR LOWER(d.titre) LIKE LOWER(CONCAT('%', :q, '%'))
                            OR (c.raisonSociale IS NOT NULL AND LOWER(c.raisonSociale) LIKE LOWER(CONCAT('%', :q, '%')))
                            OR (p.nom IS NOT NULL AND LOWER(p.nom) LIKE LOWER(CONCAT('%', :q, '%')))
                            OR (p.prenom IS NOT NULL AND LOWER(p.prenom) LIKE LOWER(CONCAT('%', :q, '%')))
                      )
                    """)
    Page<Demande> rechercher(@Param("q") String q, @Param("statut") String statut, Pageable pageable);
}
