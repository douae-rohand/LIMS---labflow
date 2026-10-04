package com.backend.modules.client.repository;

import com.backend.modules.client.entity.ClientLaboratoire;
import com.backend.modules.client.entity.ClientLaboratoireId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientLaboratoireRepository extends JpaRepository<ClientLaboratoire, ClientLaboratoireId> {

    @Query("""
            SELECT cl FROM ClientLaboratoire cl
            JOIN FETCH cl.laboratoire
            WHERE cl.utilisateur.id = :utilisateurId
            """)
    List<ClientLaboratoire> findByUtilisateur_Id(@Param("utilisateurId") Long utilisateurId);

    Optional<ClientLaboratoire> findByUtilisateur_IdAndLaboratoire_Id(Long utilisateurId, Long laboratoireId);
}
