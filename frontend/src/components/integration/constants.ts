export const TYPES_DOCUMENTS = [
  "AUTORISATION_OUVERTURE_EXPLOITATION",
  "DIPLOME_BIOLOGISTE_RESPONSABLE",
  "INSCRIPTION_ORDRE_PROFESSIONNEL",
  "CIN_BIOLOGISTE_RESPONSABLE",
  "IDENTIFICATION_JURIDIQUE",
  "JUSTIFICATIF_ADRESSE",
] as const;

export type TypeDocumentIntegration = (typeof TYPES_DOCUMENTS)[number];

export const LIBELLES_DOCUMENTS: Record<
  TypeDocumentIntegration,
  { libelle: string; raison: string }
> = {
  AUTORISATION_OUVERTURE_EXPLOITATION: {
    libelle: "Autorisation d'ouverture et d'exploitation du laboratoire",
    raison: "Vérifier que le laboratoire est autorisé",
  },
  DIPLOME_BIOLOGISTE_RESPONSABLE: {
    libelle: "Diplôme / titre du biologiste responsable",
    raison: "Vérifier la qualification du responsable",
  },
  INSCRIPTION_ORDRE_PROFESSIONNEL: {
    libelle: "Document d'inscription à l'ordre professionnel",
    raison: "Vérifier son statut professionnel",
  },
  CIN_BIOLOGISTE_RESPONSABLE: {
    libelle: "CIN du biologiste responsable",
    raison: "Vérifier son identité",
  },
  IDENTIFICATION_JURIDIQUE: {
    libelle: "Registre / document d'identification juridique du laboratoire",
    raison: "Vérifier l'existence juridique",
  },
  JUSTIFICATIF_ADRESSE: {
    libelle: "Justificatif de l'adresse du laboratoire",
    raison: "Vérifier l'adresse déclarée",
  },
};

export const TYPES_LABORATOIRE = [
  "Médical",
  "Industriel",
  "Environnemental",
  "Pharmaceutique et cosmétique",
] as const;

export const REGIONS_MAROC = [
  "Tanger-Tétouan-Al Hoceïma",
  "L'Oriental",
  "Fès-Meknès",
  "Rabat-Salé-Kénitra",
  "Béni Mellal-Khénifra",
  "Casablanca-Settat",
  "Marrakech-Safi",
  "Drâa-Tafilalet",
  "Souss-Massa",
  "Guelmim-Oued Noun",
  "Laâyoune-Sakia El Hamra",
  "Dakhla-Oued Ed-Dahab",
] as const;

export const FORMAT_DOCUMENTS_ACCEPTES = "application/pdf,image/jpeg,image/png,image/webp";
export const TAILLE_MAX_DOCUMENT = 10 * 1024 * 1024;
