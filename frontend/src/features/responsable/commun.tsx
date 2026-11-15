import { ChevronDown, RefreshCw, Search, X, type LucideIcon } from "lucide-react";
import type { ReactNode } from "react";
import { Badge, Button } from "@/components/lab";
import { ApiError } from "@/api/axios";
import { useTempsReel } from "@/features/shell/TempsReel";
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

/**
 * État de l'actualisation : « En direct » quand la connexion temps réel est active (les changements
 * arrivent seuls), sinon l'heure du dernier chargement. Le bouton « Actualiser » reste disponible.
 */
export function Actualisation({ misAJour, enCours, onActualiser }: {
  misAJour: number;
  enCours: boolean;
  onActualiser: () => void;
}) {
  const { connecte } = useTempsReel();
  const heure = misAJour
    ? new Intl.DateTimeFormat("fr-FR", { timeStyle: "medium" }).format(new Date(misAJour))
    : "—";
  return (
    <div className="flex items-center gap-3">
      {connecte ? (
        <span className="inline-flex items-center gap-1.5 text-xs font-semibold text-brand-900" aria-live="polite"
          title={`Dernier chargement à ${heure}`}>
          <span className="size-2 rounded-full bg-emerald-500" aria-hidden="true" />
          En direct
        </span>
      ) : (
        <span className="text-xs text-muted-foreground" aria-live="polite">Mis à jour à {heure}</span>
      )}
      <Button
        variant="secondary"
        size="sm"
        icon={<RefreshCw className={`size-4 ${enCours ? "animate-spin" : ""}`} aria-hidden="true" />}
        iconPosition="left"
        disabled={enCours}
        onClick={onActualiser}
      >
        Actualiser
      </Button>
    </div>
  );
}

/** Champ de recherche locale (référence, analyse, technicien, client), avec loupe et bouton d'effacement. */
export function Recherche({ valeur, onChange, placeholder, className }: {
  valeur: string;
  onChange: (valeur: string) => void;
  placeholder: string;
  className?: string;
}) {
  return (
    <div className={`relative min-w-0 ${className ?? "w-full sm:w-72"}`}>
      <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" aria-hidden="true" />
      <input
        type="search"
        value={valeur}
        onChange={(event) => onChange(event.target.value)}
        placeholder={placeholder}
        aria-label={placeholder}
        className="h-10 w-full rounded-xl border border-brand-900/15 bg-card pr-9 pl-9 text-sm text-ink-900 outline-none placeholder:text-muted-foreground focus:border-brand-900 focus:ring-2 focus:ring-brand-900/10 [&::-webkit-search-cancel-button]:hidden"
      />
      {valeur && (
        <button
          type="button"
          onClick={() => onChange("")}
          aria-label="Effacer la recherche"
          className="absolute top-1/2 right-2 flex size-6 -translate-y-1/2 items-center justify-center rounded-full text-muted-foreground hover:bg-surface-alt hover:text-ink-900"
        >
          <X className="size-3.5" aria-hidden="true" />
        </button>
      )}
    </div>
  );
}

/** Liste déroulante compacte, alignée sur le champ de recherche (même hauteur, même bordure). */
export function FiltreSelect({ libelle, valeur, onChange, options, tous, className }: {
  libelle: string;
  valeur: string;
  onChange: (valeur: string) => void;
  options: [string, string][];
  /** Libellé de l'option « sans filtre ». */
  tous: string;
  className?: string;
}) {
  const actif = valeur !== "";
  return (
    <div className={`relative shrink-0 ${className ?? "w-full sm:w-48"}`}>
      <select
        value={valeur}
        onChange={(event) => onChange(event.target.value)}
        aria-label={libelle}
        className={`h-10 w-full appearance-none truncate rounded-xl border bg-card pr-9 pl-3 text-sm outline-none focus:border-brand-900 focus:ring-2 focus:ring-brand-900/10 ${
          actif ? "border-brand-900 font-semibold text-brand-900" : "border-brand-900/15 text-ink-900"
        }`}
      >
        <option value="">{tous}</option>
        {options.map(([cle, texte]) => <option key={cle} value={cle}>{texte}</option>)}
      </select>
      <ChevronDown className="pointer-events-none absolute top-1/2 right-3 size-4 -translate-y-1/2 text-muted-foreground" aria-hidden="true" />
    </div>
  );
}

/** Onglets compacts d'une barre de filtres, avec compteur facultatif. */
export function Onglets<T extends string>({ libelle, onglets, valeur, onChange, compteurs }: {
  libelle: string;
  onglets: { id: T; libelle: string }[];
  valeur: T;
  onChange: (valeur: T) => void;
  compteurs?: Partial<Record<T, number>>;
}) {
  return (
    <div className="flex shrink-0 flex-wrap gap-1 rounded-xl bg-surface-alt p-1" role="tablist" aria-label={libelle}>
      {onglets.map((onglet) => {
        const actif = valeur === onglet.id;
        const nombre = compteurs?.[onglet.id];
        return (
          <button
            key={onglet.id}
            type="button"
            role="tab"
            aria-selected={actif}
            onClick={() => onChange(onglet.id)}
            className={`inline-flex flex-1 items-center justify-center gap-1.5 rounded-lg px-3 py-1.5 text-sm font-semibold whitespace-nowrap focus-visible:outline-2 focus-visible:outline-brand-900 lg:flex-none ${
              actif ? "bg-brand-900 text-primary-foreground shadow-sm" : "text-brand-900 hover:bg-card"
            }`}
          >
            {onglet.libelle}
            {nombre != null && (
              <span className={`rounded-full px-1.5 py-0.5 text-[11px] leading-none ${actif ? "bg-white/15" : "bg-card"}`}>{nombre}</span>
            )}
          </button>
        );
      })}
    </div>
  );
}

/** Barre de recherche et de filtres sur une seule ligne (repliée sur petit écran). */
export function BarreFiltres({ children }: { children: ReactNode }) {
  return (
    <div className="mb-4 flex flex-col gap-2 rounded-2xl border border-brand-900/10 bg-card p-2 lg:flex-row lg:items-center">
      {children}
    </div>
  );
}

export function contient(texte: string | null | undefined, recherche: string): boolean {
  return (texte ?? "").toLowerCase().includes(recherche.trim().toLowerCase());
}
