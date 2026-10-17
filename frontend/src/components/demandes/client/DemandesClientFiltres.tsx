import { RotateCcw, Search, X } from "lucide-react";
import { Button, Input, Label, Select } from "@/components/lab";
import { LIBELLE_STATUT_DEMANDE, STATUTS_DEMANDE, type StatutDemande } from "@/api/demandes";

interface DemandesClientFiltresProps {
  inputValue: string;
  statut: StatutDemande | undefined;
  filtresActifs: boolean;
  onInputChange: (value: string) => void;
  onInputClear: () => void;
  onStatutChange: (value: StatutDemande | undefined) => void;
  onReinitialiser: () => void;
}

/** Filtres de la liste « Mes demandes » (recherche locale + statut). */
export function DemandesClientFiltres({
  inputValue,
  statut,
  filtresActifs,
  onInputChange,
  onInputClear,
  onStatutChange,
  onReinitialiser,
}: DemandesClientFiltresProps) {
  return (
    <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:gap-3">
      <div className="relative min-w-[280px] flex-1">
        <Label htmlFor="mes-demandes-recherche" className="sr-only">
          Rechercher une demande
        </Label>
        <Search
          className="pointer-events-none absolute left-3.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
          aria-hidden="true"
        />
        <Input
          id="mes-demandes-recherche"
          type="search"
          placeholder="Référence, objet, laboratoire…"
          value={inputValue}
          onChange={(e) => onInputChange(e.target.value)}
          className="pl-10 pr-9"
          maxLength={100}
          autoComplete="off"
        />
        {inputValue.length > 0 && (
          <button
            type="button"
            onClick={onInputClear}
            aria-label="Effacer la recherche"
            className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-ink-900"
          >
            <X className="size-4" aria-hidden="true" />
          </button>
        )}
      </div>

      <div className="flex items-center gap-3 sm:w-auto">
        <div className="relative min-w-[200px] flex-1 sm:flex-initial">
          <Label htmlFor="mes-demandes-statut" className="sr-only">
            Filtrer par statut
          </Label>
          <Select
            id="mes-demandes-statut"
            value={statut ?? ""}
            onChange={(e) => {
              const value = e.target.value;
              onStatutChange(value === "" ? undefined : (value as StatutDemande));
            }}
          >
            <option value="">Tous les statuts</option>
            {STATUTS_DEMANDE.map((valeur) => (
              <option key={valeur} value={valeur}>
                {LIBELLE_STATUT_DEMANDE[valeur]}
              </option>
            ))}
          </Select>
        </div>

        {filtresActifs && (
          <Button
            type="button"
            variant="ghost"
            size="sm"
            icon={<RotateCcw className="size-4" aria-hidden="true" />}
            iconPosition="left"
            onClick={onReinitialiser}
            className="shrink-0"
          >
            Réinitialiser
          </Button>
        )}
      </div>
    </div>
  );
}
