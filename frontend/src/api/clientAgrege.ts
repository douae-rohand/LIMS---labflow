import { apiClient } from "./axios";
import { unwrapApi, type ApiEnvelope, type LaboratoirePublic } from "./landing";
import { ApiError } from "./axios";

/**
 * clientAgrege.ts — Types et helpers communs de l'espace client agrégé.
 * Le laboratoire est identifié par le chemin ; aucun X-Tenant-ID n'est requis.
 */

/**
 * Enveloppe des listes agrégées (tous laboratoires du client) renvoyée par
 * `GET /client/rapports`, `/client/factures` et `/client/enquetes`.
 * `laboratoiresIndisponibles` compte les laboratoires dont la lecture a échoué.
 */
export interface ListeAgregee<T> {
  elements: T[];
  laboratoiresIndisponibles: number;
}

/**
 * Liste exhaustive des laboratoires rattachés au client connecté.
 * Utilise l'endpoint /api/client/laboratoires.
 */
export async function fetchLaboratoiresClient(): Promise<LaboratoirePublic[]> {
  const envelope = await apiClient.get<ApiEnvelope<LaboratoirePublic[]>>(
    "/client/laboratoires",
  );
  return unwrapApi(envelope, "Impossible de charger vos laboratoires") ?? [];
}

/**
 * Message d'erreur destiné à l'interface cliente : retire le préfixe `[CODE]`
 * des erreurs métier (422) et traite les cas 401/403/404/429/réseau.
 *
 * Volontairement séparé de `messageErreurApi` de demandesClient.ts : ce module
 * ne dépend pas du suivi de commandes.
 */
export function messageErreurClient(error: unknown, message404: string): string {
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
