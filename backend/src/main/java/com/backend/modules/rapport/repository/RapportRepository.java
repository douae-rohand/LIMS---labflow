package com.backend.modules.rapport.repository;

import com.backend.modules.rapport.entity.Rapport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RapportRepository extends JpaRepository<Rapport, Long> {

    Optional<Rapport> findByNumero(String numero);

    Page<Rapport> findByDemande_Id(Long demandeId, Pageable pageable);

    Page<Rapport> findByStatut(String statut, Pageable pageable);

    /**
     * Rapports visibles par un client, tous laboratoires confondus (exécuté dans
     * le schéma du laboratoire). Règle de visibilité centralisée ici, seule source
     * de vérité utilisée par la liste ET le détail espace client :
     * <ul>
     *     <li>la demande appartient au client ({@code demande.client.utilisateur_id}) ;</li>
     *     <li>la demande est {@code TERMINEE} ;</li>
     *     <li>le rapport est libéré : {@code statut IN ('SIGNE','ENVOYE','ARCHIVE')}
     *         ou {@code diffuse = true} (aucune colonne « libéré » n'existe en base).</li>
     * </ul>
     * Trié par date d'émission décroissante.
     */
    @Query("""
            SELECT r FROM Rapport r
            JOIN FETCH r.demande d
            WHERE d.client.utilisateurId = :utilisateurId
              AND d.statut = 'TERMINEE'
              AND (r.statut IN ('SIGNE', 'ENVOYE', 'ARCHIVE') OR r.diffuse = true)
            ORDER BY r.dateEmission DESC
            """)
    List<Rapport> findVisiblesClient(@Param("utilisateurId") Long utilisateurId);
}
