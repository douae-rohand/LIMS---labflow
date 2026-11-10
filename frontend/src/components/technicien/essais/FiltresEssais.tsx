import { Search, X } from "lucide-react";
import { Input } from "@/components/lab";
import { cn } from "@/lib/utils";
import type { EssaisSearch } from "@/routes/app/essais/index";

/** Définition d'une puce de filtre statut. */
type PuceStatut = {
  valeur: string;
  label: string;
  compteur?: number | undefined;
};

/** Puces de statut cliquables (toggle). Le compteur est masqué si absent. */
function PucesStatut({
  statutActif,
  compteurEnRetard,
  enRetardActif,
  onStatut,
  onEnRetard,
}: {
  statutActif: string | undefined;
  compteurEnRetard: number | undefined;
  enRetardActif: boolean | undefined;
  onStatut: (v: string | undefined) => void;
  onEnRetard: (v: boolean | undefined) => void;
}) {
  const puces: PuceStatut[] = [
    { valeur: "ATTENTE_ECHANTILLON", label: "En attente" },
    { valeur: "EN_COURS", label: "En cours" },
    { valeur: "TERMINE", label: "Terminés" },
    { valeur: "NON_REALISABLE", label: "Non réalisables" },
  ];

  const puceRetard: PuceStatut = {
    valeur: "EN_RETARD",
    label: "En retard",
    compteur: compteurEnRetard,
  };

  const rendrePuce = (puce: PuceStatut, actif: boolean, onClick: () => void) => (
    <button
      key={puce.valeur}
      type="button"
      onClick={onClick}
      aria-pressed={actif}
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full border px-3 py-1.5 text-sm font-semibold transition-colors",
        "min-h-11 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-900",
        actif
          ? "bg-brand-900 text-primary-foreground border-brand-900"
          : "bg-card text-ink-900/70 border-brand-900/10 hover:bg-brand-900/5",
        puce.valeur === "EN_RETARD" && !actif && "border-red-200 text-red-700 hover:bg-red-50",
        puce.valeur === "EN_RETARD" && actif && "bg-red-700 border-red-700",
      )}
    >
      {puce.label}
      {puce.compteur !== undefined && puce.compteur > 0 && (
        <span
          className={cn(
            "inline-flex min-w-5 items-center justify-center rounded-full px-1.5 text-xs font-bold",
            actif ? "bg-accent-lime text-ink-900" : "bg-brand-900/10 text-brand-900",
          )}
        >
          {puce.compteur}
        </span>
      )}
    </button>
  );

  return (
    <div className="flex flex-wrap items-center gap-2" role="group" aria-label="Filtrer par statut">
      {rendrePuce(puceRetard, enRetardActif === true, () =>
        onEnRetard(enRetardActif === true ? undefined : true),
      )}
      {puces.map((p) =>
        rendrePuce(p, statutActif === p.valeur, () =>
          onStatut(statutActif === p.valeur ? undefined : p.valeur),
        ),
      )}
    </div>
  );
}

/** Barre de filtres : recherche + puces statut + puce en retard. */
export function FiltresEssais({
  inputValue,
  search,
  compteurEnRetard,
  onInputChange,
  onStatut,
  onEnRetard,
  onEffacerRecherche,
  onReinitialiser,
}: {
  inputValue: string;
  search: EssaisSearch;
  compteurEnRetard: number | undefined;
  onInputChange: (v: string) => void;
  onStatut: (v: string | undefined) => void;
  onEnRetard: (v: boolean | undefined) => void;
  onEffacerRecherche: () => void;
  onReinitialiser: () => void;
}) {
  const filtresActifs =
    (search.q !== undefined && search.q.length > 0) ||
    search.statut !== undefined ||
    search.enRetard === true;

  return (
    <div className="space-y-3">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
        <div className="relative flex-1">
          <Search
            className="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
            aria-hidden="true"
          />
          <Input
            type="search"
            value={inputValue}
            onChange={(e) => onInputChange(e.target.value)}
            placeholder="Rechercher par code, désignation ou numéro de demande"
            aria-label="Rechercher un essai"
            className="pl-11"
          />
          {inputValue && (
            <button
              type="button"
              onClick={onEffacerRecherche}
              aria-label="Effacer la recherche"
              className="absolute right-3 top-1/2 -translate-y-1/2 rounded-full p-1 text-muted-foreground hover:bg-muted hover:text-ink-900 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-900"
            >
              <X className="size-4" aria-hidden="true" />
            </button>
          )}
        </div>
        {filtresActifs && (
          <button
            type="button"
            onClick={onReinitialiser}
            className="self-start rounded-full border border-brand-900/10 px-4 py-2 text-sm font-semibold text-ink-900/70 hover:bg-brand-900/5 sm:self-auto"
          >
            Réinitialiser
          </button>
        )}
      </div>

      <PucesStatut
        statutActif={search.statut}
        compteurEnRetard={compteurEnRetard}
        enRetardActif={search.enRetard}
        onStatut={onStatut}
        onEnRetard={onEnRetard}
      />
    </div>
  );
}
