import apiClient, { ApiError } from "./axios";
import type { ApiResponse } from "./auth";

/**
 * paniers.ts — Panier client (base centrale, endpoints /api/client/paniers).
 * Toutes les routes exigent le rôle CLIENT ; le laboratoire est passé par le
 * chemin, l'utilisateur est déduit du jeton côté serveur.
 */

/** Une ligne de panier exposée par le backend. */
export interface LignePanier {
  essaiId: number;
  code: string | null;
  designation: string | null;
  domaineLibelle: string | null;
  tarif: number | string | null;
  dureeEstimee: number | null;
  disponible: boolean;
}

/** Résumé d'un panier (GET /api/client/paniers). */
export interface PanierResume {
  laboratoireId: number;
  laboratoireNom: string;
  nombreEssais: number;
  sousTotal: number | string | null;
  dateModification: string | null;
}

/** Détail d'un panier pour un laboratoire donné. */
export interface PanierDetail {
  laboratoireId: number;
  laboratoireNom: string;
  lignes: LignePanier[];
  sousTotal: number | string | null;
  dateModification: string | null;
}

/** Message d'erreur lisible pour les opérations de panier. */
export function messageErreurPanier(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 401) return "Session expirée, reconnectez-vous.";
    if (error.status === 403) return "Accès refusé.";
    if (error.status === 404) return "Laboratoire introuvable.";
    if (error.status === 429) return "Trop de tentatives, réessayez dans un instant.";
    if (error.status === 0 || error.message === "Network Error") {
      return "Serveur indisponible.";
    }
    if (typeof error.message === "string" && error.message.trim().length > 0) {
      return error.message.replace(/^\[[A-Z0-9_]+\]\s*/, "").trim();
    }
  }
  return "Une erreur inattendue est survenue.";
}

export async function listerPaniers(): Promise<PanierResume[]> {
  const res = await apiClient.get<ApiResponse<PanierResume[]>>("/client/paniers");
  return res.data ?? [];
}

export async function obtenirPanier(laboratoireId: number): Promise<PanierDetail> {
  const res = await apiClient.get<ApiResponse<PanierDetail>>(
    `/client/paniers/${laboratoireId}`,
  );
  return res.data;
}

export async function ajouterEssaiPanier(
  laboratoireId: number,
  essaiId: number,
): Promise<PanierDetail> {
  const res = await apiClient.post<ApiResponse<PanierDetail>>(
    `/client/paniers/${laboratoireId}/essais`,
    { essaiId },
  );
  return res.data;
}

export async function retirerEssaiPanier(
  laboratoireId: number,
  essaiId: number,
): Promise<PanierDetail> {
  const res = await apiClient.delete<ApiResponse<PanierDetail>>(
    `/client/paniers/${laboratoireId}/essais/${essaiId}`,
  );
  return res.data;
}
