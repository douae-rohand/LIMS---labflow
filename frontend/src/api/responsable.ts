import apiClient from "./axios";
import type { ApiResponse } from "./auth";

export type StatutAnalyse = "ATTENTE_ECHANTILLON" | "EN_COURS" | "TERMINE" | "NON_REALISABLE";
export type EtatValidation = "NON_SOUMIS" | "TECHNIQUE_VALIDEE" | "VALIDEE" | "RETOURNEE" | "REJETEE";
export type FiltreAnalyses = "A_AFFECTER" | "AFFECTEES" | "EN_RETARD";

/** Analyse (ligne d'essai) vue par le responsable. */
export interface AnalyseTravail {
  id: number;
  code: string;
  demandeId: number;
  demandeReference: string;
  demandeTitre?: string | null;
  demandeStatut?: string | null;
  demandeDateSoumission?: string | null;
  essaiCode?: string | null;
  essaiLibelle?: string | null;
  methode?: string | null;
  unite?: string | null;
  limiteMin?: number | null;
  limiteMax?: number | null;
  dureeEstimeeMinutes?: number | null;
  domaineCode?: string | null;
  domaineLibelle?: string | null;
  technicienId?: number | null;
  technicienNom?: string | null;
  statut: StatutAnalyse;
  dateAttribution?: string | null;
  datePrevue?: string | null;
  echeance?: string | null;
  enRetard: boolean;
  joursRetard: number;
  valeur?: string | null;
  conformite?: boolean | null;
  dateSaisie?: string | null;
  observations?: string | null;
  etatValidation: EtatValidation;
  derniereDecisionMotif?: string | null;
  derniereDecisionDate?: string | null;
  derniereDecisionPar?: string | null;
}

export interface TechnicienCharge {
  id: number;
  nomComplet: string;
  email: string;
  domaines: string[];
  aFaire: number;
  enCours: number;
  enRetard: number;
}

export interface SuiviResponsable {
  indicateurs: {
    demandesADecider: number;
    analysesAAffecter: number;
    analysesEnCours: number;
    analysesEnRetard: number;
    resultatsAValider: number;
    rapportsAPreparer: number;
  };
  techniciens: TechnicienCharge[];
  retards: AnalyseTravail[];
}

export interface ValidationHistorique {
  id: number;
  niveau: number;
  statut: "EN_ATTENTE" | "VALIDEE" | "REJETEE" | "REVISION_DEMANDEE" | null;
  commentaire?: string | null;
  dateValidation: string;
  validateurNom?: string | null;
}

export type DecisionResultat = "VALIDER" | "RETOURNER" | "REJETER";

export type StatutRapport = "BROUILLON" | "GENERE" | "SIGNE" | "ENVOYE" | "ARCHIVE";

export interface RapportTravail {
  demandeId: number;
  demandeReference: string;
  demandeTitre?: string | null;
  client?: string | null;
  nombreAnalyses: number;
  rapportId?: number | null;
  numero?: string | null;
  statut?: StatutRapport | null;
  dateEmission?: string | null;
  dateSignature?: string | null;
  signataire?: string | null;
}

// ------------------------------------------------------------- suivi
export async function lireSuivi(): Promise<SuiviResponsable> {
  const res = await apiClient.get<ApiResponse<SuiviResponsable>>("/planifications/suivi");
  return res.data;
}

// ------------------------------------------------------------- planification
export async function listerAnalyses(filtre?: FiltreAnalyses): Promise<AnalyseTravail[]> {
  const res = await apiClient.get<ApiResponse<AnalyseTravail[]>>(
    `/planifications/analyses${filtre ? `?filtre=${filtre}` : ""}`,
  );
  return res.data;
}

export async function listerTechniciens(): Promise<TechnicienCharge[]> {
  const res = await apiClient.get<ApiResponse<TechnicienCharge[]>>("/planifications/techniciens");
  return res.data;
}

export async function affecterAnalyses(
  ligneIds: number[],
  technicienId: number,
  datePrevue?: string,
): Promise<AnalyseTravail[]> {
  const res = await apiClient.post<ApiResponse<AnalyseTravail[]>>("/planifications/affectations", {
    ligneIds,
    technicienId,
    ...(datePrevue ? { datePrevue } : {}),
  });
  return res.data;
}

// ------------------------------------------------------------- validation
export async function listerResultatsAValider(): Promise<AnalyseTravail[]> {
  const res = await apiClient.get<ApiResponse<AnalyseTravail[]>>("/validations/a-valider");
  return res.data;
}

export async function historiqueValidations(ligneId: number): Promise<ValidationHistorique[]> {
  const res = await apiClient.get<ApiResponse<ValidationHistorique[]>>(`/validations/essai/${ligneId}`);
  return res.data;
}

export async function deciderResultat(
  ligneId: number,
  decision: DecisionResultat,
  motif?: string,
): Promise<AnalyseTravail> {
  const res = await apiClient.post<ApiResponse<AnalyseTravail>>(`/validations/essai/${ligneId}/decision`, {
    decision,
    ...(motif?.trim() ? { motif: motif.trim() } : {}),
  });
  return res.data;
}

// ------------------------------------------------------------- rapports
export async function listerRapportsTravail(): Promise<RapportTravail[]> {
  const res = await apiClient.get<ApiResponse<RapportTravail[]>>("/rapports/travail");
  return res.data;
}

/** Signe le rapport de la demande ; le mot de passe confirme l'identité du signataire. */
export async function signerRapportDemande(demandeId: number, motDePasse: string): Promise<RapportTravail> {
  const res = await apiClient.post<ApiResponse<RapportTravail>>(`/rapports/demande/${demandeId}/signer`, { motDePasse });
  return res.data;
}

function ouvrirBlob(blob: Blob) {
  const url = URL.createObjectURL(new Blob([blob], { type: "application/pdf" }));
  window.open(url, "_blank", "noopener,noreferrer");
  window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
}

/** Aperçu du rapport d'une demande, avant signature. */
export async function ouvrirApercuRapport(demandeId: number): Promise<void> {
  ouvrirBlob(await apiClient.getBlob(`/rapports/demande/${demandeId}/apercu`));
}

/** Ouvre le PDF signé dans un nouvel onglet. */
export async function ouvrirPdfRapport(rapportId: number): Promise<void> {
  ouvrirBlob(await apiClient.getBlob(`/rapports/${rapportId}/pdf`));
}

export async function telechargerRapport(rapportId: number, numero: string): Promise<void> {
  await apiClient.download(`/rapports/${rapportId}/pdf`, `${numero}.pdf`);
}

// ------------------------------------------------------------- signature manuscrite (profil)
export interface StatutSignature {
  presente: boolean;
  miseAJour?: string | null;
}

export async function lireStatutSignature(): Promise<StatutSignature> {
  const res = await apiClient.get<ApiResponse<StatutSignature>>("/responsable/signature/statut");
  return res.data;
}

export async function lireImageSignature(): Promise<Blob> {
  return apiClient.getBlob("/responsable/signature");
}

export async function importerSignature(fichier: File): Promise<StatutSignature> {
  const formulaire = new FormData();
  formulaire.append("fichier", fichier);
  const res = await apiClient.upload<ApiResponse<StatutSignature>>("/responsable/signature", formulaire);
  return res.data;
}

export async function supprimerSignature(): Promise<StatutSignature> {
  const res = await apiClient.delete<ApiResponse<StatutSignature>>("/responsable/signature");
  return res.data;
}
