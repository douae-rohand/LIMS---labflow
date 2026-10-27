import { Search, X, RotateCcw } from "lucide-react";
import { Button, Input, Label, Select } from "@/components/lab";
import type { StatutCatalogue } from "@/api/catalogue";

interface CatalogueFiltresProps {
  inputValue: string;
  statut: StatutCatalogue;
  filtresActifs: boolean;
  onInputChange: (value: string) => void;
  onInputClear: () => void;
  onStatutChange: (value: StatutCatalogue) => void;
  onReinitialiser: () => void;
}

export function CatalogueFiltres({
  inputValue,
  statut,
  filtresActifs,
  onInputChange,
  onInputClear,
  onStatutChange,
  onReinitialiser,
}: CatalogueFiltresProps) {
  return (
    <div className="flex flex-wrap items-center gap-3">
      <div className="relative min-w-56 flex-1">
        <Label htmlFor="catalogue-recherche" className="sr-only">
          Rechercher une analyse
        </Label>
        <Search
          className="pointer-events-none absolute top-1/2 left-3.5 size-4 -translate-y-1/2 text-muted-foreground"
          aria-hidden="true"
        />
        <Input
          id="catalogue-recherche"
          type="search"
          placeholder="Code, désignation, méthode, domaine…"
          value={inputValue}
          onChange={(e) => onInputChange(e.target.value)}
          className="pr-9 pl-10"
          maxLength={100}
          autoComplete="off"
        />
        {inputValue.length > 0 && (
          <button
            type="button"
            onClick={onInputClear}
            aria-label="Effacer la recherche"
            className="absolute top-1/2 right-3 -translate-y-1/2 text-muted-foreground hover:text-ink-900"
          >
            <X className="size-4" aria-hidden="true" />
          </button>
        )}
      </div>

      <div className="relative min-w-44">
        <Label htmlFor="catalogue-statut" className="sr-only">
          Filtrer par visibilité
        </Label>
        <Select
          id="catalogue-statut"
          value={statut}
          onChange={(e) => onStatutChange(e.target.value as StatutCatalogue)}
        >
          <option value="TOUS">Toutes les analyses</option>
          <option value="ACTIF">Visibles aux clients</option>
          <option value="INACTIF">Retirées du catalogue</option>
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
        >
          Réinitialiser
        </Button>
      )}
    </div>
  );
}
