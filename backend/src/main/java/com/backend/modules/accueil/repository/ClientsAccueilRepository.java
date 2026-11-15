package com.backend.modules.accueil.repository;

import com.backend.modules.client.entity.ClientLaboratoire;
import com.backend.modules.client.entity.ClientLaboratoireId;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Clients rattachés à un laboratoire (schéma central), avec leur compte et leur profil. */
public interface ClientsAccueilRepository extends Repository<ClientLaboratoire, ClientLaboratoireId> {

    /** Lignes [ClientLaboratoire, Utilisateur, ClientProfil (ou null)], les plus récents d'abord. */
    @Query("""
            SELECT cl, u, p FROM ClientLaboratoire cl
            JOIN cl.utilisateur u
            LEFT JOIN ClientProfil p ON p.utilisateur = u
            WHERE cl.laboratoire.id = :laboratoireId
            ORDER BY cl.datePremierContact DESC
            """)
    List<Object[]> clientsDuLaboratoire(@Param("laboratoireId") Long laboratoireId);
}
