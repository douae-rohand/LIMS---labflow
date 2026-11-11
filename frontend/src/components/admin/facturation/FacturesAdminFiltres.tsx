import { RotateCcw, Search, X } from "lucide-react";
import type { ReactNode } from "react";
import { Button, Input, Label, Select } from "@/components/lab";

export type StatutFactureAdmin = "EMISE" | "PAYEE" | "EN_RETARD" | "IMPAYEE" | "ANNULEE";

interface FacturesAdminFiltresProps {
  recherche: string;
  statut: StatutFactureAdmin | undefined;
  onRechercheChange: (value: string) => void;
  onRechercheClear: () => void;
  onStatutChange: (value: StatutFactureAdmin | undefined) => void;
  onReinitialiser: () => void;
  actions?: ReactNode;
}

export function FacturesAdminFiltres({
  recherche,
  statut,
  onRechercheChange,
  onRechercheClear,
  onStatutChange,
  onReinitialiser,
  actions,
}: FacturesAdminFiltresProps) {
  const filtreActif = Boolean(recherche || statut !== undefined);

  return (
    <div className="mb-5 flex flex-wrap items-center gap-3">
      <div className="relative min-w-56 flex-1">
        <Label htmlFor="recherche-facture" className="sr-only">
          Rechercher une facture
        </Label>
        <Search
          className="pointer-events-none absolute left-3.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
          aria-hidden="true"
        />
        <Input
          id="recherche-facture"
          type="search"
          value={recherche}
          onChange={(event) => onRechercheChange(event.target.value)}
          placeholder="Numéro ou client"
          className="pl-10 pr-9"
          maxLength={100}
          autoComplete="off"
        />
        {recherche.length > 0 && (
          <button
            type="button"
            onClick={onRechercheClear}
            aria-label="Effacer la recherche"
            className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-ink-900"
          >
            <X className="size-4" aria-hidden="true" />
          </button>
        )}
      </div>
      <div className="relative min-w-44">
        <Label htmlFor="filtre-statut-facture" className="sr-only">Filtrer par statut de facture</Label>
        <Select
          id="filtre-statut-facture"
          value={statut ?? ""}
          onChange={(event) => onStatutChange(
            event.target.value === "" ? undefined : event.target.value as StatutFactureAdmin,
          )}
        >
          <option value="">Tous les statuts</option>
          <option value="EMISE">Émise</option>
          <option value="PAYEE">Payée</option>
          <option value="EN_RETARD">En retard</option>
          <option value="IMPAYEE">Impayée</option>
        </Select>
      </div>
      {filtreActif && (
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
      {actions}
    </div>
  );
}
