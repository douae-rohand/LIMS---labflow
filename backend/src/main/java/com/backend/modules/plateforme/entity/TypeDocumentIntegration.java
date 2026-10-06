package com.backend.modules.plateforme.entity;

/**
 * Les six documents officiels exigés pour l'intégration d'un laboratoire.
 */
public enum TypeDocumentIntegration {
    AUTORISATION_OUVERTURE_EXPLOITATION(
            "Autorisation d'ouverture et d'exploitation du laboratoire",
            "Vérifier que le laboratoire est autorisé"),
    DIPLOME_BIOLOGISTE_RESPONSABLE(
            "Diplôme / titre du biologiste responsable",
            "Vérifier la qualification du responsable"),
    INSCRIPTION_ORDRE_PROFESSIONNEL(
            "Document d'inscription à l'ordre professionnel",
            "Vérifier son statut professionnel"),
    CIN_BIOLOGISTE_RESPONSABLE(
            "CIN du biologiste responsable",
            "Vérifier son identité"),
    IDENTIFICATION_JURIDIQUE(
            "Registre / document d'identification juridique du laboratoire",
            "Vérifier l'existence juridique"),
    JUSTIFICATIF_ADRESSE(
            "Justificatif de l'adresse du laboratoire",
            "Vérifier l'adresse déclarée");

    private final String libelle;
    private final String raison;

    TypeDocumentIntegration(String libelle, String raison) {
        this.libelle = libelle;
        this.raison = raison;
    }

    public String getLibelle() {
        return libelle;
    }

    public String getRaison() {
        return raison;
    }
}
