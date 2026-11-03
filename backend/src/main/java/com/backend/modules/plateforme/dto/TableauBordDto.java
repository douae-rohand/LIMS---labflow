package com.backend.modules.plateforme.dto;

import java.util.List;

public record TableauBordDto(
        long utilisateursActifs,
        long totalDemandesAnalyse,
        int laboratoiresIndisponibles,
        int laboratoiresExclus,
        List<PointSerie> inscriptions,
        List<ActiviteLaboratoire> activites,
        List<PointSerie> domaines,
        List<AlerteIntegration> alertesIntegration,
        String alertesN8n,
        List<EtatService> services
) {
    public record PointSerie(String cle, long nombre) {}

    /**
     * couverture : INCLUS (laboratoire actif interrogé), EXCLU (suspendu ou désactivé, non interrogé),
     * INACCESSIBLE (actif, schéma ou requête en échec). Un tenant inaccessible n'est pas une panne de plateforme.
     */
    public record ActiviteLaboratoire(
            Long laboratoireId,
            String nom,
            String statut,
            String couverture,
            Long nombre,
            List<PointSerie> parStatut,
            long utilisateurs,
            Integer tauxCloture,
            Long tailleOctets,
            String versionSchema,
            String etatSchema,
            List<PointSerie> demandesParMois,
            boolean indisponible
    ) {}

    public record AlerteIntegration(String numero, String nom, long jours) {}

    public record EtatService(String nom, String etat) {}
}
