import apiClient from "./axios";
import type { ApiResponse } from "./auth";
import { messageErreurClient } from "./clientAgrege";

/**
 * syntheseClient.ts — Synthèse du tableau de bord client
 * (endpoint /api/client/synthese), tous laboratoires confondus.
 */

export interface SyntheseClient {
  rapportsDisponibles: number;
  facturesImpayees: number;
  /** Somme des restes à payer des factures impayées. */
  montantRestantDu: number | string | null;
  enquetesEnAttente: number;
  laboratoiresIndisponibles: number;
}

/** Synthèse du tableau de bord : aucune 404 attendue. */
export function messageErreurSynthese(error: unknown): string {
  return messageErreurClient(error, "Impossible de charger la synthèse.");
}

export async function chargerSyntheseClient(): Promise<SyntheseClient> {
  const res = await apiClient.get<ApiResponse<SyntheseClient>>("/client/synthese");
  return res.data;
}
