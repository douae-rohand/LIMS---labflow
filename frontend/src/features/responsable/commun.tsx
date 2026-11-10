import type { LucideIcon } from "lucide-react";
import type { ReactNode } from "react";
import { Badge } from "@/components/lab";
import { ApiError } from "@/api/axios";
import type { AnalyseTravail, EtatValidation, StatutAnalyse } from "@/api/responsable";

export function EnTeteResponsable({
  icone: Icone,
  titre,
  description,
  action,
}: {
  icone: LucideIcon;
  titre: string;
  description: string;
  action?: ReactNode;
}) {
  return (
    <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
      <div className="flex items-center gap-3">
        <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-brand-900/10 text-brand-900">
          <Icone className="size-5" aria-hidden="true" />
        </div>
        <div>
          <h1 className="text-2xl font-bold text-ink-900">{titre}</h1>
          <p className="text-sm text-muted-foreground">{description}</p>
        </div>
      </div>
      {action}
    </div>
  );
}

export function formaterDate(valeur?: string | null, avecHeure = false): string {
  if (!valeur) return "—";
  const date = new Date(valeur);
  if (Number.isNaN(date.getTime())) return "—";
  return new Intl.DateTimeFormat("fr-FR", avecHeure
    ? { dateStyle: "medium", timeStyle: "short" }
    : { dateStyle: "medium" }).format(date);
}

export function messageErreur(erreur: unknown, defaut: string): string {
  if (erreur instanceof ApiError) {
    return erreur.message.replace(/^\[[A-Z0-9_]+\]\s*/, "") || defaut;
  }
  return defaut;
}

const LIBELLES_STATUT: Record<StatutAnalyse, string> = {
  ATTENTE_ECHANTILLON: "En attente d'échantillon",
  EN_COURS: "En cours",
  TERMINE: "Résultat saisi",
  NON_REALISABLE: "Non réalisable",
};

export function BadgeStatutAnalyse({ analyse }: { analyse: AnalyseTravail }) {
  if (analyse.enRetard) {
    return <Badge size="sm" variant="dark">En retard · {analyse.joursRetard} j</Badge>;
  }
  return (
    <Badge size="sm" variant={analyse.statut === "EN_COURS" ? "lime" : analyse.statut === "TERMINE" ? "mint" : "outline"}>
      {LIBELLES_STATUT[analyse.statut]}
    </Badge>
  );
}

export const LIBELLES_VALIDATION: Record<EtatValidation, string> = {
  NON_SOUMIS: "Non soumis",
  TECHNIQUE_VALIDEE: "Validé par le technicien",
  VALIDEE: "Validé",
  RETOURNEE: "Retourné au technicien",
  REJETEE: "Rejeté — à refaire",
};

/** Valeurs de référence lisibles : « 0,7 – 1,1 g/L ». */
export function reference(analyse: AnalyseTravail): string {
  const { limiteMin: min, limiteMax: max, unite } = analyse;
  const format = (n: number) => n.toLocaleString("fr-FR");
  let texte = "—";
  if (min != null && max != null) texte = `${format(min)} – ${format(max)}`;
  else if (min != null) texte = `≥ ${format(min)}`;
  else if (max != null) texte = `≤ ${format(max)}`;
  return texte !== "—" && unite ? `${texte} ${unite}` : texte;
}

export function EtatChargement({ chargement, erreur, vide, messageVide, onReessayer }: {
  chargement: boolean;
  erreur: boolean;
  vide: boolean;
  messageVide: string;
  onReessayer?: () => void;
}) {
  if (chargement) return <p className="py-8 text-sm text-muted-foreground">Chargement…</p>;
  if (erreur) {
    return (
      <div className="rounded-2xl border border-dashed border-brand-900/15 bg-card px-4 py-10 text-center">
        <p className="text-sm font-semibold text-ink-900">Les données n'ont pas pu être chargées.</p>
        {onReessayer && (
          <button type="button" onClick={onReessayer} className="mt-2 text-sm font-semibold text-brand-900 underline-offset-4 hover:underline">
            Réessayer
          </button>
        )}
      </div>
    );
  }
  if (vide) {
    return (
      <div className="rounded-2xl border border-dashed border-brand-900/15 bg-card px-4 py-10 text-center">
        <p className="text-sm font-semibold text-ink-900">{messageVide}</p>
      </div>
    );
  }
  return null;
}
