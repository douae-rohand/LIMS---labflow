import apiClient, { ApiError } from "./axios";
import type { ApiResponse } from "./auth";
import { messageErreurClient, type ListeAgregee } from "./clientAgrege";

/**
 * enquetesClient.ts — Mes enquêtes de satisfaction (/api/client/enquetes).
 *
 * La liste est triée côté serveur (non répondues d'abord). Seule la note
 * globale est obligatoire pour répondre ; les critères et le commentaire sont
 * facultatifs.
 */

export interface EnqueteResumeClient {
  enqueteId: number;
  laboratoireId: number;
  laboratoireNom: string | null;
  demandeReference: string | null;
  demandeTitre: string | null;
  dateEnvoi: string | null;
  repondue: boolean;
  dateReponse: string | null;
  noteGlobale: number | null;
}

export interface EnqueteDetailClient extends EnqueteResumeClient {
  demandeId: number | null;
  noteDelai: number | null;
  noteClarte: number | null;
  noteRelation: number | null;
  commentaire: string | null;
}

export interface RepondreEnqueteBody {
  noteGlobale: number;
  noteDelai?: number;
  noteClarte?: number;
  noteRelation?: number;
  commentaire?: string;
}

/** Liste « Mes enquêtes » : aucune 404 attendue. */
export function messageErreurEnquetes(error: unknown): string {
  return messageErreurClient(error, "Impossible de charger vos enquêtes.");
}

/** Détail d'une enquête : 404 = enquête introuvable. */
export function messageErreurEnquete(error: unknown): string {
  return messageErreurClient(error, "Enquête introuvable.");
}

/** Réponse à une enquête : 422 ENQUETE_DEJA_REPONDUE, 404, 400 validation. */
export function messageErreurReponse(error: unknown): string {
  if (error instanceof ApiError && error.code === "ENQUETE_DEJA_REPONDUE") {
    return "Vous avez déjà répondu à cette enquête.";
  }
  return messageErreurClient(error, "Enquête introuvable.");
}

export async function listerEnquetesClient(): Promise<ListeAgregee<EnqueteResumeClient>> {
  const res = await apiClient.get<ApiResponse<ListeAgregee<EnqueteResumeClient>>>(
    "/client/enquetes",
  );
  return res.data;
}

export async function detailEnqueteClient(
  laboratoireId: number,
  enqueteId: number,
): Promise<EnqueteDetailClient> {
  const res = await apiClient.get<ApiResponse<EnqueteDetailClient>>(
    `/client/enquetes/${laboratoireId}/${enqueteId}`,
  );
  return res.data;
}

export async function repondreEnqueteClient(
  laboratoireId: number,
  enqueteId: number,
  body: RepondreEnqueteBody,
): Promise<EnqueteDetailClient> {
  const res = await apiClient.post<ApiResponse<EnqueteDetailClient>>(
    `/client/enquetes/${laboratoireId}/${enqueteId}/repondre`,
    body,
  );
  return res.data;
}
