package com.backend.modules.rapport.repository;

import com.backend.modules.rapport.entity.RapportLigne;
import com.backend.modules.rapport.entity.RapportLigneId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RapportLigneRepository extends JpaRepository<RapportLigne, RapportLigneId> {

    /**
     * Lignes d'un rapport avec la ligne d'essai et l'essai chargés en une requête
     * (désignation et unité proviennent de {@code essai}). Évite tout N+1.
     */
    @Query("""
            SELECT rl FROM RapportLigne rl
            JOIN FETCH rl.ligneEssai le
            JOIN FETCH le.essai
            WHERE rl.rapport.id = :rapportId
            """)
    List<RapportLigne> findByRapport_Id(@Param("rapportId") Long rapportId);
}
