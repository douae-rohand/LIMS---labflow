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
  essaiCode?: string | null;
  essaiLibelle?: string | null;
  methode?: string | null;
  unite?: string | null;
  limiteMin?: number | null;
  limiteMax?: number | null;
  dureeEstimeeJours?: number | null;
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
  empreinte?: string | null;
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

export async function genererRapport(demandeId: number): Promise<void> {
  await apiClient.post<ApiResponse<unknown>>(`/rapports/generer/${demandeId}`);
}

/** Signe et libère le rapport ; le mot de passe confirme l'identité du signataire. */
export async function signerRapport(rapportId: number, motDePasse: string): Promise<RapportTravail> {
  const res = await apiClient.post<ApiResponse<RapportTravail>>(`/rapports/${rapportId}/signer`, { motDePasse });
  return res.data;
}

// ------------------------------------------------------------- vérification publique
export interface VerificationRapport {
  laboratoire: string;
  numero: string;
  statut: string;
  dateSignature?: string | null;
  signataire?: string | null;
  fichierAuthentique?: boolean | null;
  signatureValide?: boolean | null;
  documentComplet?: boolean | null;
  horodatage?: string | null;
  certificat?: string | null;
}

export async function statutRapportPublic(laboratoire: string, numero: string): Promise<VerificationRapport> {
  const params = new URLSearchParams({ laboratoire, numero });
  const res = await apiClient.get<ApiResponse<VerificationRapport>>(`/public/rapports/verification?${params.toString()}`);
  return res.data;
}

export async function verifierFichierRapport(
  laboratoire: string,
  numero: string,
  fichier: File,
): Promise<VerificationRapport> {
  const formulaire = new FormData();
  formulaire.append("laboratoire", laboratoire);
  formulaire.append("numero", numero);
  formulaire.append("fichier", fichier);
  const res = await apiClient.upload<ApiResponse<VerificationRapport>>("/public/rapports/verification", formulaire);
  return res.data;
}

/** Ouvre l'aperçu (projet) ou le PDF signé dans un nouvel onglet. */
export async function ouvrirPdfRapport(rapportId: number, signe: boolean): Promise<void> {
  const blob = await apiClient.getBlob(`/rapports/${rapportId}/${signe ? "pdf" : "apercu"}`);
  const url = URL.createObjectURL(new Blob([blob], { type: "application/pdf" }));
  window.open(url, "_blank", "noopener,noreferrer");
  window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
}

export async function telechargerRapport(rapportId: number, numero: string): Promise<void> {
  await apiClient.download(`/rapports/${rapportId}/pdf`, `${numero}.pdf`);
}
