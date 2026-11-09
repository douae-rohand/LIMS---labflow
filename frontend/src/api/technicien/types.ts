/**
 * types.ts — Types TypeScript alignés sur les DTO backend du module technicien.
 *
 * Conventions de mapping backend → TypeScript :
 *  - Instant  → string ISO-8601 (ex. "2026-11-09T22:32:11.868Z")
 *  - LocalDate → string "YYYY-MM-DD"
 *  - BigDecimal → number | null
 *  - Long / Integer → number
 *
 * Référence backend : com.backend.modules.technicien.dto.*
 */

// ---------------------------------------------------------------------------
// Références
// ---------------------------------------------------------------------------

export interface DomaineRefDto {
  code: string;
  libelle: string;
}

export interface EssaiRefDto {
  code: string;
  designation: string;
  methode: string | null;
  unite: string | null;
  dureeEstimee: number | null;
  domaine: DomaineRefDto | null;
}

export interface EchantillonRefDto {
  reference: string;
  nature: string;
  conformite: boolean | null;
  etatConservationConstate: string | null;
}

export interface ExigenceEchantillonRefDto {
  nature: string;
  etatConservation: string;
  quantiteMinimale: number | null;
  unite: string | null;
}

export interface ValidationTechniqueDto {
  id: number;
  niveau: number;
  decision: string;
  motif: string | null;
  dateValidation: string;
}

// ---------------------------------------------------------------------------
// Liste / planning
// ---------------------------------------------------------------------------

/** Statut d'une ligne d'essai (null possible en base). */
export type StatutEssai = "ATTENTE_ECHANTILLON" | "EN_COURS" | "TERMINE" | "NON_REALISABLE";

/** Classification calculée par le serveur. */
export type NiveauConformite = "CONFORME" | "NON_CONFORME" | "CRITIQUE" | "NON_EVALUE";

export interface LigneEssaiTechnicienDto {
  ligneEssaiId: number;
  code: string;
  essai: EssaiRefDto;
  demandeNumero: string;
  demandeTitre: string;
  echantillon: EchantillonRefDto | null;
  statut: string | null;
  dateAttribution: string;
  echeanceEstimee: string | null;
  enRetard: boolean;
  valeur: string | null;
  niveauConformite: string;
}

export interface LigneEssaiDetailTechnicienDto {
  ligneEssaiId: number;
  code: string;
  essai: EssaiRefDto;
  demandeNumero: string;
  demandeTitre: string;
  echantillon: EchantillonRefDto | null;
  statut: string | null;
  dateAttribution: string;
  dateDebut: string | null;
  dateFin: string | null;
  dateSaisie: string | null;
  echeanceEstimee: string | null;
  enRetard: boolean;
  valeur: string | null;
  niveauConformite: string;
  dureeMinutes: number | null;
  limiteMin: number | null;
  limiteMax: number | null;
  seuilCritiqueMin: number | null;
  seuilCritiqueMax: number | null;
  exigencesEchantillon: ExigenceEchantillonRefDto[];
  historiqueValidations: ValidationTechniqueDto[];
}

// ---------------------------------------------------------------------------
// Planning & synthèse
// ---------------------------------------------------------------------------

export interface PlanningJourDto {
  date: string;
  essais: LigneEssaiTechnicienDto[];
}

export interface ChargeSemaineDto {
  date: string;
  count: number;
}

export interface SyntheseTechnicienDto {
  essaisAujourdhuiCount: number;
  enCoursCount: number;
  enRetardCount: number;
  enAttenteEchantillonCount: number;
  terminesSemaineCount: number;
  validationsTechniquesAttenteCount: number;
  chargeSemaine: ChargeSemaineDto[];
}

// ---------------------------------------------------------------------------
// Import CSV
// ---------------------------------------------------------------------------

export interface ImportLigneErreurDto {
  numeroLigne: number;
  colonne: string;
  code: string;
  message: string;
}

export interface ImportApercuLigneDto {
  numeroLigne: number;
  codeLigne: string;
  valeur: string;
  commentaire: string | null;
  niveauConformite: string;
}

export interface ImportApercuResponse {
  lignesLues: number;
  lignesOk: number;
  lignesErreur: number;
  pret: boolean;
  erreurs: ImportLigneErreurDto[];
  apercu: ImportApercuLigneDto[];
}

export interface ImportConfirmerResponse {
  lignesImportees: number;
  erreurs: ImportLigneErreurDto[];
}

// ---------------------------------------------------------------------------
// Corps de requêtes (mutations)
// ---------------------------------------------------------------------------

export interface ResultatSaisiePayload {
  valeur: string;
  commentaire?: string;
}

export interface NonRealisablePayload {
  motif: string;
}

export interface TempsPayload {
  debut?: string;
  fin?: string;
  dureeMinutes?: number;
}

export interface ValidationTechniquePayload {
  commentaire?: string;
}

// ---------------------------------------------------------------------------
// Paramètres de recherche (liste)
// ---------------------------------------------------------------------------

export interface ListerEssaisParams {
  page?: number;
  size?: number;
  tri?: string;
  direction?: "asc" | "desc";
  statut?: StatutEssai | undefined;
  domaineId?: number | undefined;
  demandeId?: number | undefined;
  debut?: string | undefined;
  fin?: string | undefined;
  enRetard?: boolean | undefined;
  q?: string | undefined;
}
