import { Activity } from "lucide-react";

/** Seuil d'alerte, identique au serveur (AnomalieService.SEUIL_ALERTE). */
export const SEUIL_ALERTE_ANOMALIE = 40;

const LIBELLES: Record<string, string> = {
  CRITIQUE: "Critique",
  HORS_INTERVALLE: "Hors intervalle (OOS)",
  HORS_TENDANCE: "Inhabituel (OOT)",
  NON_CONFORME: "Non conforme",
};

export function libelleAnomalie(type: string | null | undefined): string {
  return type ? (LIBELLES[type] ?? type) : "Aucune anomalie";
}

/**
 * Score d'anomalie d'un résultat (0 à 100) avec son explication au survol.
 * Rien n'est affiché sous le seuil d'alerte, pour ne pas charger la file.
 */
export function BadgeAnomalie({ score, type, motif }: {
  score?: number | null | undefined;
  type?: string | null | undefined;
  motif?: string | null | undefined;
}) {
  if (score == null || score < SEUIL_ALERTE_ANOMALIE) return null;
  const fort = score >= 80;
  return (
    <span
      title={motif ?? undefined}
      className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[11px] font-semibold ${
        fort ? "bg-rose-100 text-rose-800" : "bg-amber-100 text-amber-900"
      }`}
    >
      <Activity className="size-3" aria-hidden="true" />
      {libelleAnomalie(type)} · {score}/100
      {motif && <span className="sr-only"> : {motif}</span>}
    </span>
  );
}
