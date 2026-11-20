package com.backend.modules.ia.donnees;

import com.backend.modules.plateforme.entity.StatutIntegration;
import com.backend.modules.plateforme.repository.DemandeIntegrationRepository;
import com.backend.modules.plateforme.repository.LaboratoireRepository;
import org.springframework.ai.tool.annotation.Tool;

/** Outils du Super Admin : totaux de la plateforme (schéma central), sans donnée personnelle. */
public final class OutilsSuperAdmin {

    private final SessionOutils session;
    private final LaboratoireRepository laboratoireRepository;
    private final DemandeIntegrationRepository demandeIntegrationRepository;

    OutilsSuperAdmin(SessionOutils session, LaboratoireRepository laboratoireRepository,
                     DemandeIntegrationRepository demandeIntegrationRepository) {
        this.session = session;
        this.laboratoireRepository = laboratoireRepository;
        this.demandeIntegrationRepository = demandeIntegrationRepository;
    }

    @Tool(description = "État de la plateforme : laboratoires actifs et suspendus, demandes d'intégration "
            + "en attente, en attente de compléments, acceptées et refusées.")
    public String etatPlateforme() {
        return session.lire("Gestion des laboratoires", "/app/gestion-laboratoires", () ->
                "Laboratoires actifs : " + laboratoireRepository.countByStatut("ACTIF")
                        + "\nLaboratoires suspendus : " + laboratoireRepository.countByStatut("SUSPENDU")
                        + "\nLaboratoires désactivés : " + laboratoireRepository.countByStatut("DESACTIVE")
                        + "\nDemandes d'intégration en attente : " + integrations(StatutIntegration.EN_ATTENTE)
                        + "\nDemandes en attente de compléments : " + integrations(StatutIntegration.COMPLEMENTS_DEMANDES)
                        + "\nDemandes acceptées : " + integrations(StatutIntegration.ACCEPTEE)
                        + "\nDemandes refusées : " + integrations(StatutIntegration.REFUSEE));
    }

    private long integrations(StatutIntegration statut) {
        return demandeIntegrationRepository.countByStatut(statut.name());
    }
}
