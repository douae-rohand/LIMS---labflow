import apiClient, { ApiError } from "./axios";
import type { ApiResponse } from "./auth";
import type { StatutDemande } from "./demandes";

/**
 * demandesClient.ts — Espace client : catalogue → panier → commande,
 * puis suivi des demandes (endpoints /api/client/demandes).
 * Le laboratoire est identifié par le chemin ; aucun X-Tenant-ID n'est requis.
 */

export interface LigneEssaiClient {
  essaiId: number | null;
  code: string | null;
  designation: string | null;
  domaineLibelle: string | null;
  tarif: number | string | null;
  statut: string | null;
}

export interface EchantillonClient {
  reference: string | null;
  nature: string | null;
  conformite: boolean | null;
  motif: string | null;
}

export interface HistoriqueEntree {
  action: string | null;
  statutAvant: string | null;
  statutApres: string | null;
  motif: string | null;
  dateAction: string | null;
  acteur: string | null;
}

export interface DemandeDetailClient {
  id: number;
  reference: string;
  titre: string;
  objectif: string | null;
  statut: StatutDemande;
  dateSoumission: string | null;
  motif: string | null;
  laboratoireNom: string;
  lignes: LigneEssaiClient[];
  total: number | string | null;
  echantillons: EchantillonClient[];
  historique: HistoriqueEntree[];
}

/** Ligne de la liste « Mes demandes » (tous laboratoires). */
export interface DemandeResumeClient {
  id: number;
  reference: string;
  objet: string | null;
  statut: StatutDemande;
  laboratoireId: number | null;
  laboratoireCode: string | null;
  laboratoireNom: string | null;
  total: number | string | null;
  dateSoumission: string | null;
}

function messageErreurApi(error: unknown, message404: string): string {
  if (error instanceof ApiError) {
    if (error.status === 401) return "Session expirée, reconnectez-vous.";
    if (error.status === 403) return "Accès refusé.";
    if (error.status === 404) return message404;
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

/** Création depuis le panier : 404 = laboratoire introuvable. */
export function messageErreurDemandeClient(error: unknown): string {
  return messageErreurApi(error, "Laboratoire introuvable.");
}

/** Liste « Mes demandes » : aucune 404 attendue. */
export function messageErreurListeClient(error: unknown): string {
  return messageErreurApi(error, "Impossible de charger vos demandes.");
}

/** Détail d'une demande : 404 = demande introuvable. */
export function messageErreurDetailClient(error: unknown): string {
  return messageErreurApi(error, "Demande introuvable.");
}

/** Annulation : 404 = demande introuvable ; refus métier explicite. */
export function messageErreurAnnulation(error: unknown): string {
  if (error instanceof ApiError && error.code === "ANNULATION_IMPOSSIBLE") {
    return "Cette demande ne peut plus être annulée, le laboratoire l'a déjà traitée.";
  }
  return messageErreurApi(error, "Demande introuvable.");
}

export async function creerDemandeDepuisPanier(
  laboratoireId: number,
  payload: { titre: string; objectif?: string | undefined },
): Promise<DemandeDetailClient> {
  const body: { titre: string; objectif?: string } = { titre: payload.titre };
  if (payload.objectif !== undefined && payload.objectif.trim().length > 0) {
    body.objectif = payload.objectif.trim();
  }
  const res = await apiClient.post<ApiResponse<DemandeDetailClient>>(
    `/client/demandes/${laboratoireId}`,
    body,
  );
  return res.data;
}

export async function listerDemandesClient(): Promise<DemandeResumeClient[]> {
  const res = await apiClient.get<ApiResponse<DemandeResumeClient[]>>("/client/demandes");
  return res.data;
}

export async function detailDemandeClient(
  laboratoireId: number,
  demandeId: number,
): Promise<DemandeDetailClient> {
  const res = await apiClient.get<ApiResponse<DemandeDetailClient>>(
    `/client/demandes/${laboratoireId}/${demandeId}`,
  );
  return res.data;
}

export async function annulerDemandeClient(
  laboratoireId: number,
  demandeId: number,
  motif: string,
): Promise<DemandeDetailClient> {
  const res = await apiClient.post<ApiResponse<DemandeDetailClient>>(
    `/client/demandes/${laboratoireId}/${demandeId}/annuler`,
    { motif },
  );
  return res.data;
}
