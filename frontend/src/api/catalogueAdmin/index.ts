import apiClient, { ApiError } from "../axios";
import type { ApiResponse } from "../auth";
import type { PageResponse } from "../utilisateurs";
import * as Types from "./types";

export type {
  EssaiAdminDto,
  ExigenceEchantillonDto,
  DomaineDto,
  CreerEssaiRequest,
  ModifierEssaiRequest,
  CreerDomaineRequest,
  ModifierDomaineRequest,
  CreerExigenceRequest,
  ModifierExigenceRequest,
  CatalogueStatsDto,
  ListerEssaisParams,
} from "./types";

export function messageErreurCatalogueAdmin(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 429) return "Trop de tentatives, réessayez dans un instant.";
    if (error.status === 403) return "Accès refusé.";
    if (error.status === 404) return "Élément introuvable.";
    if (error.status === 0 || error.message === "Network Error") return "Serveur indisponible.";
    if (error.status === 400) return error.message.replace(/^\[[A-Z0-9_]+\]\s*/, "").trim();
    if (typeof error.message === "string" && error.message.trim().length > 0) {
      return error.message.replace(/^\[[A-Z0-9_]+\]\s*/, "").trim();
    }
  }
  return "Une erreur inattendue est survenue.";
}

export async function listerEssais(params?: Types.ListerEssaisParams): Promise<PageResponse<Types.EssaiAdminDto>> {
  const queryParams: Record<string, string | number | boolean> = {};
  if (params) {
    if (params.domaineId !== undefined) queryParams["domaineId"] = params.domaineId;
    if (params.actif !== undefined) queryParams["actif"] = params.actif;
    if (params.q !== undefined && params.q.trim().length > 0) queryParams["q"] = params.q.trim();
    if (params.page !== undefined && params.page >= 0) queryParams["page"] = params.page;
    if (params.size !== undefined && params.size > 0) queryParams["size"] = Math.min(params.size, 50);
  }
  const res = await apiClient.get<ApiResponse<PageResponse<Types.EssaiAdminDto>>>(
    "/catalogue-admin/essais",
    { params: queryParams },
  );
  return res.data;
}

export async function getEssai(id: number): Promise<Types.EssaiAdminDto> {
  const res = await apiClient.get<ApiResponse<Types.EssaiAdminDto>>(`/catalogue-admin/essais/${id}`);
  return res.data;
}

export async function creerEssai(request: Types.CreerEssaiRequest): Promise<Types.EssaiAdminDto> {
  const res = await apiClient.post<ApiResponse<Types.EssaiAdminDto>>("/catalogue-admin/essais", request);
  return res.data;
}

export async function modifierEssai(id: number, request: Types.ModifierEssaiRequest): Promise<Types.EssaiAdminDto> {
  const res = await apiClient.put<ApiResponse<Types.EssaiAdminDto>>(`/catalogue-admin/essais/${id}`, request);
  return res.data;
}

export async function activerEssai(id: number): Promise<void> {
  await apiClient.patch(`/catalogue-admin/essais/${id}/activer`);
}

export async function desactiverEssai(id: number): Promise<void> {
  await apiClient.patch(`/catalogue-admin/essais/${id}/desactiver`);
}

export async function supprimerEssai(id: number): Promise<void> {
  await apiClient.delete(`/catalogue-admin/essais/${id}`);
}

export async function listerDomaines(): Promise<Types.DomaineDto[]> {
  const res = await apiClient.get<ApiResponse<Types.DomaineDto[]>>("/catalogue-admin/domaines");
  return res.data;
}

export async function creerDomaine(request: Types.CreerDomaineRequest): Promise<Types.DomaineDto> {
  const res = await apiClient.post<ApiResponse<Types.DomaineDto>>("/catalogue-admin/domaines", request);
  return res.data;
}

export async function modifierDomaine(id: number, request: Types.ModifierDomaineRequest): Promise<Types.DomaineDto> {
  const res = await apiClient.put<ApiResponse<Types.DomaineDto>>(`/catalogue-admin/domaines/${id}`, request);
  return res.data;
}

export async function supprimerDomaine(id: number): Promise<void> {
  await apiClient.delete(`/catalogue-admin/domaines/${id}`);
}

export async function listerExigences(essaiId: number): Promise<Types.ExigenceEchantillonDto[]> {
  const res = await apiClient.get<ApiResponse<Types.ExigenceEchantillonDto[]>>(`/catalogue-admin/exigences/${essaiId}`);
  return res.data;
}

export async function suggestionsExigences(): Promise<Types.ExigenceEchantillonDto[]> {
  const res = await apiClient.get<ApiResponse<Types.ExigenceEchantillonDto[]>>("/catalogue-admin/exigences/suggestions");
  return res.data;
}

export async function creerExigence(essaiId: number, request: Types.CreerExigenceRequest): Promise<Types.ExigenceEchantillonDto> {
  const res = await apiClient.post<ApiResponse<Types.ExigenceEchantillonDto>>(
    "/catalogue-admin/exigences",
    request,
    { params: { essaiId } },
  );
  return res.data;
}

export async function modifierExigence(id: number, request: Types.ModifierExigenceRequest): Promise<Types.ExigenceEchantillonDto> {
  const res = await apiClient.put<ApiResponse<Types.ExigenceEchantillonDto>>(`/catalogue-admin/exigences/${id}`, request);
  return res.data;
}

export async function supprimerExigence(id: number): Promise<void> {
  await apiClient.delete(`/catalogue-admin/exigences/${id}`);
}

export async function getStats(): Promise<Types.CatalogueStatsDto> {
  const res = await apiClient.get<ApiResponse<Types.CatalogueStatsDto>>("/catalogue-admin/stats");
  return res.data;
}
