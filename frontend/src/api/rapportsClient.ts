import apiClient from "./axios";
import type { ApiResponse } from "./auth";
import { messageErreurClient, type ListeAgregee } from "./clientAgrege";

/**
 * rapportsClient.ts — Mes rapports (endpoints /api/client/rapports).
 * Lecture seule ; le téléchargement PDF n'existe pas encore (telechargeable=false).
 */

export interface RapportResumeClient {
  rapportId: number;
  laboratoireId: number;
  laboratoireNom: string | null;
  demandeId: number | null;
  demandeReference: string | null;
  demandeTitre: string | null;
  type: string | null;
  version: number | null;
  dateEmission: string | null;
  /** Toujours false : aucun stockage PDF côté serveur. */
  telechargeable: boolean;
}

export interface RapportLigneClient {
  ligneEssaiId: number | null;
  designation: string | null;
  valeur: string | null;
  unite: string | null;
  conformite: boolean | null;
}

export interface RapportDetailClient extends RapportResumeClient {
  dateSignature: string | null;
  lignes: RapportLigneClient[];
  syntheseIa: string | null;
  mentionSyntheseIa: string | null;
}

/** Liste « Mes rapports » : aucune 404 attendue. */
export function messageErreurRapports(error: unknown): string {
  return messageErreurClient(error, "Impossible de charger vos rapports.");
}

/** Détail d'un rapport : 404 = rapport introuvable. */
export function messageErreurRapport(error: unknown): string {
  return messageErreurClient(error, "Rapport introuvable.");
}

export async function listerRapportsClient(): Promise<ListeAgregee<RapportResumeClient>> {
  const res = await apiClient.get<ApiResponse<ListeAgregee<RapportResumeClient>>>(
    "/client/rapports",
  );
  return res.data;
}

export async function detailRapportClient(
  laboratoireId: number,
  rapportId: number,
): Promise<RapportDetailClient> {
  const res = await apiClient.get<ApiResponse<RapportDetailClient>>(
    `/client/rapports/${laboratoireId}/${rapportId}`,
  );
  return res.data;
}
