import apiClient from "./axios";
import type { ApiResponse } from "./auth";
import { messageErreurClient, type ListeAgregee } from "./clientAgrege";

/**
 * rapportsClient.ts — Mes rapports (endpoints /api/client/rapports).
 * Lecture seule ; téléchargement PDF disponible pour les rapports signés.
 */

export interface RapportResumeClient {
  rapportId: number;
  laboratoireId: number;
  laboratoireNom: string | null;
  demandeId: number | null;
  demandeReference: string | null;
  demandeTitre: string | null;
  version: number | null;
  dateEmission: string | null;
  /** true si le rapport est signé et disponible au téléchargement. */
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

export async function telechargerRapportClient(
  laboratoireId: number,
  rapportId: number,
  numero: string,
): Promise<void> {
  await apiClient.download(
    `/client/rapports/${laboratoireId}/${rapportId}/pdf`,
    `Rapport-${numero}.pdf`,
  );
}
