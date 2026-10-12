import { Search, X, RotateCcw } from "lucide-react";
import { Button, Input, Label, Select } from "@/components/lab";
import { LIBELLE_STATUT_DEMANDE, STATUTS_DEMANDE, type StatutDemande } from "@/api/demandes";

interface DemandesFiltresProps {
  inputValue: string;
  statut: StatutDemande | undefined;
  filtresActifs: boolean;
  onInputChange: (value: string) => void;
  onInputClear: () => void;
  onStatutChange: (value: StatutDemande | undefined) => void;
  onReinitialiser: () => void;
}

export function DemandesFiltres({
  inputValue,
  statut,
  filtresActifs,
  onInputChange,
  onInputClear,
  onStatutChange,
  onReinitialiser,
}: DemandesFiltresProps) {
  return (
    <div className="flex flex-wrap items-center gap-3">
      <div className="relative min-w-56 flex-1">
        <Label htmlFor="demandes-recherche" className="sr-only">
          Rechercher une demande
        </Label>
        <Search
          className="pointer-events-none absolute left-3.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
          aria-hidden="true"
        />
        <Input
          id="demandes-recherche"
          type="search"
          placeholder="Référence, objet, client…"
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

      <div className="relative min-w-44">
        <Label htmlFor="demandes-statut" className="sr-only">
          Filtrer par statut
        </Label>
        <Select
          id="demandes-statut"
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
        >
          Réinitialiser
        </Button>
      )}
    </div>
  );
}
