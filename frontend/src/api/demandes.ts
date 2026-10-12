import apiClient, { ApiError } from "./axios";
import type { ApiResponse } from "./auth";
import type { PageResponse } from "./utilisateurs";

export type StatutDemande =
  | "BROUILLON"
  | "SOUMISE"
  | "ACCEPTEE"
  | "EN_COURS"
  | "TERMINEE"
  | "ANNULEE"
  | "REJETEE";

export const STATUTS_DEMANDE: StatutDemande[] = [
  "BROUILLON",
  "SOUMISE",
  "ACCEPTEE",
  "EN_COURS",
  "TERMINEE",
  "ANNULEE",
  "REJETEE",
];

export const LIBELLE_STATUT_DEMANDE: Record<StatutDemande, string> = {
  BROUILLON: "Brouillon",
  SOUMISE: "En attente",
  ACCEPTEE: "Acceptée",
  EN_COURS: "En cours",
  TERMINEE: "Terminée",
  ANNULEE: "Annulée",
  REJETEE: "Refusée",
};

export interface DemandeResume {
  id: number;
  reference: string;
  objet: string;
  statut: StatutDemande;
  demandeur: string | null;
  dateSoumission: string;
  dateDecision: string | null;
  decisionPossible: boolean;
}

export interface IndicateursDemandes {
  enAttente: number;
  acceptees: number;
  refusees: number;
  total: number;
}

export interface ClientDemande {
  id: number;
  code: string;
  raisonSociale: string;
  ice: string | null;
  adresse: string | null;
  nomComplet: string | null;
  email: string | null;
  telephone: string | null;
}

export interface PatientDemande {
  id: number;
  code: string;
  nomComplet: string | null;
  dateNaissance: string | null;
  telephone: string | null;
  email: string | null;
}

export interface AnalyseDemandee {
  id: number;
  code: string;
  designation: string | null;
  methode: string | null;
  unite: string | null;
  tarif: number | null;
  montant: number | null;
  statut: string | null;
}

export interface EchantillonDemande {
  id: number;
  reference: string;
  nature: string | null;
  statut: string | null;
  dateReception: string | null;
  conformite: boolean | null;
}

export interface DocumentDemande {
  id: number;
  code: string;
  nomFichier: string | null;
  typeMime: string | null;
  taille: number | null;
  dateAjout: string | null;
}

export interface DemandeLaboratoire {
  id: number;
  reference: string;
  objet: string;
  objectif: string | null;
  statut: StatutDemande;
  dateSoumission: string;
  dateDecision: string | null;
  motif: string | null;
  decideurId: number | null;
  decideurNom: string | null;
  decisionPossible: boolean;
  client: ClientDemande | null;
  patient: PatientDemande | null;
  analyses: AnalyseDemandee[];
  echantillons: EchantillonDemande[];
  documents: DocumentDemande[];
}

export interface ListerDemandesParams {
  q?: string | undefined;
  statut?: StatutDemande | undefined;
  page?: number | undefined;
  size?: number | undefined;
}

export function messageErreurDemande(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 429) {
      return "Trop de tentatives, réessayez dans un instant.";
    }
    if (error.status === 403) {
      return "Accès refusé.";
    }
    if (error.status === 404) {
      return "Demande introuvable.";
    }
    if (error.status === 400 && error.data && typeof error.data === "object") {
      const champs = error.data as Record<string, unknown>;
      if (typeof champs["motif"] === "string" && champs["motif"].trim().length > 0) {
        return champs["motif"];
      }
    }
    if (error.status === 0 || error.message === "Network Error") {
      return "Serveur indisponible.";
    }
    if (typeof error.message === "string" && error.message.trim().length > 0) {
      return error.message.replace(/^\[[A-Z0-9_]+\]\s*/, "").trim();
    }
  }
  return "Une erreur inattendue est survenue.";
}

export async function listerDemandesLaboratoire(
  params?: ListerDemandesParams,
): Promise<PageResponse<DemandeResume>> {
  const queryParams: Record<string, string | number> = {};
  if (params?.q !== undefined && params.q.trim().length > 0) {
    queryParams["q"] = params.q.trim();
  }
  if (params?.statut) {
    queryParams["statut"] = params.statut;
  }
  if (params?.page !== undefined && params.page >= 0) {
    queryParams["page"] = params.page;
  }
  if (params?.size !== undefined && params.size > 0) {
    queryParams["size"] = Math.min(params.size, 50);
  }
  const res = await apiClient.get<ApiResponse<PageResponse<DemandeResume>>>(
    "/demandes/laboratoire",
    { params: queryParams },
  );
  return res.data;
}

export async function indicateursDemandes(): Promise<IndicateursDemandes> {
  const res = await apiClient.get<ApiResponse<IndicateursDemandes>>(
    "/demandes/laboratoire/indicateurs",
  );
  return res.data;
}

export async function detailDemandeLaboratoire(id: number): Promise<DemandeLaboratoire> {
  const res = await apiClient.get<ApiResponse<DemandeLaboratoire>>(
    `/demandes/laboratoire/${id}`,
  );
  return res.data;
}

export async function accepterDemande(id: number): Promise<DemandeLaboratoire> {
  const res = await apiClient.post<ApiResponse<DemandeLaboratoire>>(
    `/demandes/laboratoire/${id}/accepter`,
  );
  return res.data;
}

export async function refuserDemande(id: number, motif: string): Promise<DemandeLaboratoire> {
  const res = await apiClient.post<ApiResponse<DemandeLaboratoire>>(
    `/demandes/laboratoire/${id}/refuser`,
    { motif },
  );
  return res.data;
}
