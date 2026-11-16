import { Search, X } from "lucide-react";
import { Input, Select } from "@/components/lab";
import type { EssaisSearch } from "@/routes/app/essais/index";

/** Barre de filtres : recherche et menu de statut sur une même ligne. */
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
    <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
      <div className="relative min-w-0 flex-1">
        <Search
          className="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
          aria-hidden="true"
        />
        <Input
          type="search"
          value={inputValue}
          onChange={(event) => onInputChange(event.target.value)}
          placeholder="Rechercher par code, désignation ou numéro de demande"
          aria-label="Rechercher un essai"
          className="min-h-14 rounded-full pl-11"
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

      <Select
        aria-label="Filtrer les essais par statut"
        value={search.enRetard === true ? "EN_RETARD" : search.statut ?? ""}
        onChange={(event) => {
          const valeur = event.target.value;
          if (valeur === "EN_RETARD") {
            onStatut(undefined);
            onEnRetard(true);
          } else {
            onEnRetard(undefined);
            onStatut(valeur || undefined);
          }
        }}
        className="min-h-14 w-full rounded-full bg-card sm:w-56"
      >
        <option value="">Tous les statuts</option>
        <option value="EN_RETARD">
          En retard{compteurEnRetard !== undefined ? ` (${compteurEnRetard})` : ""}
        </option>
        <option value="ATTENTE_ECHANTILLON">En attente</option>
        <option value="EN_COURS">En cours</option>
        <option value="TERMINE">Terminés</option>
        <option value="NON_REALISABLE">Non réalisables</option>
      </Select>

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
  );
}
