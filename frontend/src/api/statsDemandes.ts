import apiClient, { ApiError } from "./axios";
import type { ApiResponse } from "./auth";

export interface DemandesStatsDto {
  total: number;
  parStatut: Record<string, number>;
  nombreSurPeriode: number;
  tauxCloture: number;
  serieMensuelle: SerieMensuelleDemandeDto[];
}

export interface SerieMensuelleDemandeDto {
  mois: string;
  valeur: number;
}

export function messageErreurStatsDemandes(error: unknown): string {
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

export async function getStats(debut?: string, fin?: string): Promise<DemandesStatsDto> {
  const params: Record<string, string> = {};
  if (debut !== undefined) params["debut"] = debut;
  if (fin !== undefined) params["fin"] = fin;
  const res = await apiClient.get<ApiResponse<DemandesStatsDto>>("/demandes/stats", { params });
  return res.data;
}
