/**
 * libelles.ts — Libellés partagés de l'espace technicien.
 *
 * Toutes les fonctions replient sur le code brut en cas de valeur inconnue,
 * sans jamais planter.
 */

import { format, formatDistanceToNow, parseISO } from "date-fns";
import { fr } from "date-fns/locale";

// ---------------------------------------------------------------------------
// Statut de ligne d'essai
// ---------------------------------------------------------------------------

export const LIBELLE_STATUT_ESSAI: Record<string, string> = {
  ATTENTE_ECHANTILLON: "En attente d'échantillon",
  EN_COURS: "En cours",
  TERMINE: "Terminé",
  NON_REALISABLE: "Non réalisable",
};

/** Classes Tailwind pour le badge de statut (texte + fond). */
export const CLASSE_STATUT_ESSAI: Record<string, string> = {
  ATTENTE_ECHANTILLON: "bg-amber-50 text-amber-700 border-amber-200",
  EN_COURS: "bg-blue-50 text-blue-700 border-blue-200",
  TERMINE: "bg-green-50 text-green-700 border-green-200",
  NON_REALISABLE: "bg-muted text-muted-foreground border-border",
};

export function libelleStatutEssai(statut: string | null | undefined): string {
  if (!statut) return "Inconnu";
  return LIBELLE_STATUT_ESSAI[statut] ?? statut;
}

export function classeStatutEssai(statut: string | null | undefined): string {
  if (!statut) return "bg-muted text-muted-foreground border-border";
  return CLASSE_STATUT_ESSAI[statut] ?? "bg-muted text-muted-foreground border-border";
}

// ---------------------------------------------------------------------------
// Niveau de conformité (calcul serveur)
// ---------------------------------------------------------------------------

export const LIBELLE_CONFORMITE: Record<string, string> = {
  CONFORME: "Conforme",
  NON_CONFORME: "Non conforme",
  CRITIQUE: "Critique",
  NON_EVALUE: "Non évalué",
};

export const CLASSE_CONFORMITE: Record<string, string> = {
  CONFORME: "bg-green-50 text-green-700 border-green-200",
  NON_CONFORME: "bg-amber-50 text-amber-700 border-amber-200",
  CRITIQUE: "bg-red-50 text-red-700 border-red-200",
  NON_EVALUE: "bg-muted text-muted-foreground border-border",
};

export function libelleConformite(niveau: string | null | undefined): string {
  if (!niveau) return "Non évalué";
  return LIBELLE_CONFORMITE[niveau] ?? niveau;
}

export function classeConformite(niveau: string | null | undefined): string {
  if (!niveau) return "bg-muted text-muted-foreground border-border";
  return CLASSE_CONFORMITE[niveau] ?? "bg-muted text-muted-foreground border-border";
}

// ---------------------------------------------------------------------------
// Décision de validation (historique)
// ---------------------------------------------------------------------------

export const LIBELLE_DECISION: Record<string, string> = {
  VALIDEE: "Validée",
  REJETEE: "Rejetée",
  REVISION_DEMANDEE: "Révision demandée",
  EN_ATTENTE: "En attente",
};

export function libelleDecision(decision: string | null | undefined): string {
  if (!decision) return "—";
  return LIBELLE_DECISION[decision] ?? decision;
}

export function libelleNiveauValidation(niveau: number | null | undefined): string {
  if (niveau === 1) return "Validation technique";
  if (niveau === 2) return "Validation responsable";
  return `Niveau ${niveau ?? "?"}`;
}

// ---------------------------------------------------------------------------
// Dates et durées (français, sans décalage de fuseau sur les LocalDate)
// ---------------------------------------------------------------------------

/** Formate un Instant ISO en date + heure française. */
export function formatDateHeure(instant: string | null | undefined): string {
  if (!instant) return "—";
  try {
    return format(parseISO(instant), "dd MMM yyyy 'à' HH:mm", { locale: fr });
  } catch {
    return instant;
  }
}

/** Formate un Instant ISO en date française courte. */
export function formatDate(instant: string | null | undefined): string {
  if (!instant) return "—";
  try {
    return format(parseISO(instant), "dd MMM yyyy", { locale: fr });
  } catch {
    return instant;
  }
}

/** Formate un Instant ISO en durée relative (ex. « il y a 3 heures »). */
export function formatDateRelative(instant: string | null | undefined): string {
  if (!instant) return "—";
  try {
    return formatDistanceToNow(parseISO(instant), { locale: fr, addSuffix: true });
  } catch {
    return instant;
  }
}

/**
 * Formate un LocalDate ("YYYY-MM-DD") sans décalage de fuseau.
 * On construit manuellement la date pour éviter `new Date("YYYY-MM-DD")`
 * qui interprète l'heure comme UTC minuit.
 */
export function formatDateLocale(localDate: string | null | undefined): string {
  if (!localDate) return "—";
  try {
    const [annee, mois, jour] = localDate.split("-").map((p) => Number(p));
    if (annee === undefined || mois === undefined || jour === undefined) return localDate;
    const date = new Date(annee, mois - 1, jour);
    return format(date, "EEE dd MMM yyyy", { locale: fr });
  } catch {
    return localDate;
  }
}

/** Formate une durée en minutes en texte lisible (ex. « 1 h 05 min », « 45 min »). */
export function formaterDuree(minutes: number | null | undefined): string {
  if (minutes === null || minutes === undefined) return "—";
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  if (h > 0 && m > 0) return `${h} h ${String(m).padStart(2, "0")} min`;
  if (h > 0) return `${h} h`;
  return `${m} min`;
}
