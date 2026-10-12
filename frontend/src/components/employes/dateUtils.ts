import type { RoleEmploye } from "@/api/employes";

export const LIBELLE_ROLE: Record<RoleEmploye | string, string> = {
  RESPONSABLE: "Responsable",
  TECHNICIEN: "Technicien",
  ACCUEIL: "Agent d'accueil",
};

/**
 * Formatage de date utilisé dans la liste des employés.
 * Affiche la date sous forme "12 oct. 2026", ou "Jamais" si nulle/vide.
 */
export function formatDate(iso: string | null): string {
  if (!iso) return "Jamais";
  return new Date(iso).toLocaleDateString("fr-FR", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  });
}

/**
 * Formatage de date et heure pour les invitations et échéances précises.
 * Affiche "12 oct. 2026 à 14:30" ou "Date inconnue".
 */
export function formatDateTime(iso: string | null): string {
  if (!iso) return "Date inconnue";
  return new Date(iso).toLocaleDateString("fr-FR", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}
