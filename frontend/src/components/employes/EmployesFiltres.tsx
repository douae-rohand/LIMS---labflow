import { Search, X, RotateCcw, ChevronDown } from "lucide-react";
import { Button, Input, Label } from "@/components/lab";
import type { StatutEmploye, RoleEmploye, TriEmploye, DirectionTri } from "@/api/employes";

const LIBELLE_ROLE: Record<RoleEmploye, string> = {
  RESPONSABLE: "Responsable",
  TECHNICIEN: "Technicien",
  ACCUEIL: "Agent d'accueil",
};

export interface FiltresState {
  q: string | undefined;
  statut: StatutEmploye | undefined;
  role: RoleEmploye | undefined;
  tri: TriEmploye;
  direction: DirectionTri;
}

interface EmployesFiltresProps {
  /** Valeur actuelle du champ de texte (état local, pas forcément l'URL) */
  inputValue: string;
  statut: StatutEmploye | undefined;
  role: RoleEmploye | undefined;
  filtresActifs: boolean;
  onInputChange: (value: string) => void;
  onInputClear: () => void;
  onStatutChange: (value: StatutEmploye | undefined) => void;
  onRoleChange: (value: RoleEmploye | undefined) => void;
  onReinitialiser: () => void;
}

export function EmployesFiltres({
  inputValue,
  statut,
  role,
  filtresActifs,
  onInputChange,
  onInputClear,
  onStatutChange,
  onRoleChange,
  onReinitialiser,
}: EmployesFiltresProps) {
  return (
    <div className="flex flex-wrap items-center gap-3">
      {/* Champ de recherche */}
      <div className="relative min-w-56 flex-1">
        <Label htmlFor="employes-recherche" className="sr-only">
          Rechercher un employé
        </Label>
        <Search
          className="pointer-events-none absolute left-3.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
          aria-hidden="true"
        />
        <Input
          id="employes-recherche"
          type="search"
          placeholder="Matricule, nom, prénom, e-mail…"
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

      {/* Filtre Statut */}
      <div className="relative">
        <Label htmlFor="employes-statut" className="sr-only">
          Filtrer par statut
        </Label>
        <select
          id="employes-statut"
          value={statut ?? ""}
          onChange={(e) => {
            const v = e.target.value as StatutEmploye | "";
            onStatutChange(v === "" ? undefined : v);
          }}
          className="h-11 appearance-none rounded-2xl border border-input bg-card pl-4 pr-9 text-sm font-medium text-ink-900 transition-colors duration-200 focus:border-brand-600 focus:outline-none"
        >
          <option value="">Tous les statuts</option>
          <option value="ACTIF">Actif</option>
          <option value="EN_ATTENTE">En attente</option>
          <option value="DESACTIVE">Désactivé</option>
        </select>
        <ChevronDown
          className="pointer-events-none absolute right-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
          aria-hidden="true"
        />
      </div>

      {/* Filtre Fonction */}
      <div className="relative">
        <Label htmlFor="employes-role" className="sr-only">
          Filtrer par fonction
        </Label>
        <select
          id="employes-role"
          value={role ?? ""}
          onChange={(e) => {
            const v = e.target.value as RoleEmploye | "";
            onRoleChange(v === "" ? undefined : v);
          }}
          className="h-11 appearance-none rounded-2xl border border-input bg-card pl-4 pr-9 text-sm font-medium text-ink-900 transition-colors duration-200 focus:border-brand-600 focus:outline-none"
        >
          <option value="">Toutes les fonctions</option>
          {(Object.entries(LIBELLE_ROLE) as [RoleEmploye, string][]).map(([k, l]) => (
            <option key={k} value={k}>
              {l}
            </option>
          ))}
        </select>
        <ChevronDown
          className="pointer-events-none absolute right-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
          aria-hidden="true"
        />
      </div>

      {/* Bouton réinitialiser */}
      {filtresActifs && (
        <Button
          type="button"
          variant="secondary"
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
