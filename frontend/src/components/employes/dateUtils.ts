import type { RoleEmploye } from "@/api/employes";

export const LIBELLE_ROLE: Record<RoleEmploye | string, string> = {
  RESPONSABLE: "Responsable",
  TECHNICIEN: "Technicien",
  ACCUEIL: "Agent d'accueil",
};

/**
 * Formatage de date standard (ex : 12 oct. 2026).
 * Renvoie une chaîne vide si la date est nulle ou indéfinie.
 */
export function formatDate(iso: string | null | undefined): string {
  if (!iso) return "";
  return new Date(iso).toLocaleDateString("fr-FR", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  });
}

/**
 * Formatage de dernière connexion pour les listes et détails.
 * Renvoie explicitement « Jamais » si l'utilisateur ne s'est encore jamais connecté.
 */
export function formatDerniereConnexion(iso: string | null | undefined): string {
  if (!iso) return "Jamais";
  return new Date(iso).toLocaleDateString("fr-FR", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  });
}

/**
 * Formatage de date et heure pour les invitations et échéances précises.
 * Ex : "12 oct. 2026 à 14:30".
 */
export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return "Date inconnue";
  return new Date(iso).toLocaleString("fr-FR", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}
