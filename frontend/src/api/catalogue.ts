import apiClient, { ApiError } from "./axios";
import type { ApiResponse } from "./auth";
import type { PageResponse } from "./utilisateurs";

export type StatutCatalogue = "ACTIF" | "INACTIF" | "TOUS";

export interface AnalyseCatalogue {
  id: number;
  code: string;
  designation: string;
  description: string | null;
  normeReference: string | null;
  dureeEstimee: number | null;
  prixUnitaire: number | string | null;
  uniteMesure: string | null;
  domaineCode: string | null;
  domaineLibelle: string | null;
  actif: boolean;
}

export interface DomaineCatalogue {
  code: string;
  libelle: string;
}

export interface EnregistrerAnalysePayload {
  code: string;
  designation: string;
  description?: string | undefined;
  normeReference?: string | undefined;
  dureeEstimee: number;
  prixUnitaire?: number | undefined;
  uniteMesure?: string | undefined;
  domaineCode?: string | undefined;
}

export interface ListerCatalogueParams {
  q?: string | undefined;
  statut?: StatutCatalogue | undefined;
  page?: number | undefined;
  size?: number | undefined;
}

export function messageErreurCatalogue(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 403) {
      return "Vous ne pouvez gérer que le catalogue de votre laboratoire.";
    }
    if (error.status === 404) {
      return "Analyse introuvable dans votre catalogue.";
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

export async function listerCatalogue(
  params?: ListerCatalogueParams,
): Promise<PageResponse<AnalyseCatalogue>> {
  const queryParams: Record<string, string | number> = {
    statut: params?.statut ?? "TOUS",
    page: params?.page ?? 0,
    size: params?.size ?? 10,
  };
  if (params?.q !== undefined && params.q.trim().length > 0) {
    queryParams["q"] = params.q.trim();
  }
  const res = await apiClient.get<ApiResponse<PageResponse<AnalyseCatalogue>>>("/catalogue", {
    params: queryParams,
  });
  return res.data;
}

export async function listerDomainesCatalogue(): Promise<DomaineCatalogue[]> {
  const res = await apiClient.get<ApiResponse<DomaineCatalogue[]>>("/catalogue/domaines");
  return res.data ?? [];
}

export async function creerAnalyse(payload: EnregistrerAnalysePayload): Promise<AnalyseCatalogue> {
  const res = await apiClient.post<ApiResponse<AnalyseCatalogue>>("/catalogue", payload);
  return res.data;
}

export async function modifierAnalyse(
  id: number,
  payload: EnregistrerAnalysePayload,
): Promise<AnalyseCatalogue> {
  const res = await apiClient.put<ApiResponse<AnalyseCatalogue>>(`/catalogue/${id}`, payload);
  return res.data;
}

export async function activerAnalyse(id: number): Promise<AnalyseCatalogue> {
  const res = await apiClient.patch<ApiResponse<AnalyseCatalogue>>(`/catalogue/${id}/activer`);
  return res.data;
}

export async function desactiverAnalyse(id: number): Promise<AnalyseCatalogue> {
  const res = await apiClient.patch<ApiResponse<AnalyseCatalogue>>(`/catalogue/${id}/desactiver`);
  return res.data;
}
