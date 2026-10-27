package com.backend.modules.demande.repository;

import com.backend.modules.demande.entity.Demande;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DemandeRepository extends JpaRepository<Demande, Long> {

    Optional<Demande> findByNumero(String numero);

    Page<Demande> findByClient_Id(Long clientId, Pageable pageable);

    Page<Demande> findByClient_UtilisateurId(Long utilisateurId, Pageable pageable);

    java.util.List<Demande> findByClient_UtilisateurId(Long utilisateurId);

    Optional<Demande> findByIdAndClient_UtilisateurId(Long id, Long utilisateurId);

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
                    WHERE (:filtrerStatut = false OR d.statut IN :statuts)
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
                    WHERE (:filtrerStatut = false OR d.statut IN :statuts)
                      AND (
                            :q = ''
                            OR LOWER(d.numero) LIKE LOWER(CONCAT('%', :q, '%'))
                            OR LOWER(d.titre) LIKE LOWER(CONCAT('%', :q, '%'))
                            OR (c.raisonSociale IS NOT NULL AND LOWER(c.raisonSociale) LIKE LOWER(CONCAT('%', :q, '%')))
                            OR (p.nom IS NOT NULL AND LOWER(p.nom) LIKE LOWER(CONCAT('%', :q, '%')))
                            OR (p.prenom IS NOT NULL AND LOWER(p.prenom) LIKE LOWER(CONCAT('%', :q, '%')))
                      )
                    """)
    Page<Demande> rechercher(
            @Param("q") String q,
            @Param("filtrerStatut") boolean filtrerStatut,
            @Param("statuts") Collection<String> statuts,
            Pageable pageable);

    /**
     * Retourne le numéro de séquence maximal pour un préfixe de numéro donné
     * (ex. {@code 202610-}). Le parsing se fait en SQL (SUBSTRING + CAST),
     * jamais en Java. Retourne 0 si aucune demande ne porte ce préfixe.
     */
    @Query(value = """
            SELECT COALESCE(MAX(CAST(SUBSTRING(numero, 8) AS UNSIGNED)), 0)
            FROM demande WHERE numero LIKE :prefix
            """, nativeQuery = true)
    long findMaxSequenceForPrefix(@Param("prefix") String prefix);

    /**
     * Annule atomiquement une demande uniquement si son statut courant est
     * {@code NOUVEAU} (ou un alias persistant). Retourne le nombre de lignes
     * effectivement mises à jour (0 = la demande n'était pas NOUVEAU).
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE Demande d
            SET d.statut = 'ANNULEE', d.motif = :motif
            WHERE d.id = :id AND d.statut IN :statutsNouveau
            """)
    int annulerSiNouvelle(@Param("id") Long id,
                          @Param("motif") String motif,
                          @Param("statutsNouveau") Collection<String> statutsNouveau);
}
