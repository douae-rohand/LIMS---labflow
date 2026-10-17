import { LIBELLE_STATUT_DEMANDE } from "@/api/demandes";
import type { HistoriqueEntree } from "@/api/demandesClient";

/**
 * Libellés français partagés de l'espace client « Mes demandes ».
 * Aucun statut n'est jamais affiché sous forme de code brut sans repli.
 */

/**
 * Statut d'une ligne d'essai. Toutes les valeurs réelles de l'enum
 * `StatutEssai` (ATTENTE_ECHANTILLON, EN_COURS, TERMINE, NON_REALISABLE)
 * sont couvertes ; `null` correspond à une ligne non encore traitée.
 */
export const LIBELLE_STATUT_ESSAI: Record<string, string> = {
  ATTENTE_ECHANTILLON: "En attente d'échantillon",
  EN_COURS: "En cours",
  TERMINE: "Terminé",
  NON_REALISABLE: "Non réalisable",
};

export function libelleStatutEssai(statut: string | null | undefined): string {
  if (statut == null || statut === "") return "En attente";
  return LIBELLE_STATUT_ESSAI[statut] ?? statut;
}

export function libelleConformite(conformite: boolean | null | undefined): string {
  if (conformite == null) return "Non évaluée";
  return conformite ? "Conforme" : "Non conforme";
}

/** Libellés des actions du journal d'audit ; repli = code brut. */
export const LIBELLE_ACTION_AUDIT: Record<string, string> = {
  CREATION_DEMANDE: "Création de la demande",
  ANNULATION_DEMANDE: "Annulation de la demande",
  ACCEPTATION_DEMANDE: "Acceptation par le laboratoire",
  REFUS_DEMANDE: "Refus par le laboratoire",
};

export function libelleAction(action: string | null | undefined): string {
  if (action == null || action === "") return "Action";
  return LIBELLE_ACTION_AUDIT[action] ?? action;
}

/** Libellé d'un statut de demande ; repli = code brut, jamais de plantage. */
export function libelleStatutDemande(statut: string | null | undefined): string {
  if (statut == null || statut === "") return "—";
  return (LIBELLE_STATUT_DEMANDE as Record<string, string>)[statut] ?? statut;
}

/** Date lisible ; « — » si absente ou invalide. */
export function formatDate(valeur: string | null | undefined): string {
  if (valeur == null || valeur === "") return "—";
  const date = new Date(valeur);
  if (Number.isNaN(date.getTime())) return "—";
  return new Intl.DateTimeFormat("fr-FR", { dateStyle: "medium" }).format(date);
}

/**
 * Date d'arrêt d'une demande (annulation ou refus), lue dans le journal
 * d'audit de la demande. Retourne null si l'entrée correspondante est absente
 * ou si sa date n'est pas renseignée.
 */
export function dateArretDemande(historique: HistoriqueEntree[]): string | null {
  const entree = historique.find(
    (item) => item.action === "ANNULATION_DEMANDE" || item.action === "REFUS_DEMANDE",
  );
  return entree?.dateAction ?? null;
}
