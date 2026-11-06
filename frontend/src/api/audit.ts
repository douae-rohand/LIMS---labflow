import apiClient, { ApiError } from "./axios";
import type { ApiResponse } from "./auth";
import type { PageResponse } from "./utilisateurs";

export interface AuditEntryDto {
  code: string;
  dateAction: string;
  action: string;
  objet: string | null;
  valeurAvant: string | null;
  valeurApres: string | null;
  motif: string | null;
  acteurNom: string;
  acteurRole: string;
}

export interface ListerAuditParams {
  acteur?: string | undefined;
  role?: string | undefined;
  action?: string | undefined;
  typeObjet?: string | undefined;
  debut?: string | undefined;
  fin?: string | undefined;
  q?: string | undefined;
  page?: number | undefined;
  size?: number | undefined;
}

export function messageErreurAudit(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 429) return "Trop de tentatives, réessayez dans un instant.";
    if (error.status === 403) return "Accès refusé.";
    if (error.status === 0 || error.message === "Network Error") return "Serveur indisponible.";
    if (error.status === 400) return error.message.replace(/^\[[A-Z0-9_]+\]\s*/, "").trim();
    if (typeof error.message === "string" && error.message.trim().length > 0) {
      return error.message.replace(/^\[[A-Z0-9_]+\]\s*/, "").trim();
    }
  }
  return "Une erreur inattendue est survenue.";
}

export async function listerAudit(params?: ListerAuditParams): Promise<PageResponse<AuditEntryDto>> {
  const queryParams: Record<string, string | number> = {};
  if (params) {
    if (params.acteur !== undefined && params.acteur.trim().length > 0) queryParams["acteur"] = params.acteur.trim();
    if (params.role !== undefined && params.role.trim().length > 0) queryParams["role"] = params.role.trim();
    if (params.action !== undefined && params.action.trim().length > 0) queryParams["action"] = params.action.trim();
    if (params.typeObjet !== undefined && params.typeObjet.trim().length > 0) queryParams["typeObjet"] = params.typeObjet.trim();
    if (params.debut !== undefined && params.debut.trim().length > 0) queryParams["debut"] = params.debut;
    if (params.fin !== undefined && params.fin.trim().length > 0) queryParams["fin"] = params.fin;
    if (params.q !== undefined && params.q.trim().length > 0) queryParams["q"] = params.q.trim();
    if (params.page !== undefined && params.page >= 0) queryParams["page"] = params.page;
    if (params.size !== undefined && params.size > 0) queryParams["size"] = Math.min(params.size, 50);
  }
  const res = await apiClient.get<ApiResponse<PageResponse<AuditEntryDto>>>("/audit", { params: queryParams });
  return res.data;
}
