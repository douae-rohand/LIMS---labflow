import { Label, Select, Input } from "@/components/lab";
import type { StatutFactureClient } from "@/api/facturesClient";
import { Search } from "lucide-react";

interface FacturesClientFiltresProps {
  statut: StatutFactureClient | "RETARD" | undefined;
  laboratoireId: number | undefined;
  recherche: string | undefined;
  laboratoires: { id: number; nom: string }[];
  onStatutChange: (valeur: StatutFactureClient | "RETARD" | undefined) => void;
  onLaboratoireChange: (valeur: number | undefined) => void;
  onRechercheChange: (valeur: string | undefined) => void;
}

/** Filtres de la liste « Mes factures » : statut, labo et recherche. */
export function FacturesClientFiltres({
  statut,
  laboratoireId,
  recherche,
  laboratoires,
  onStatutChange,
  onLaboratoireChange,
  onRechercheChange,
}: FacturesClientFiltresProps) {
  return (
    <div className="flex flex-col gap-4 sm:flex-row sm:items-end">
      {/* Recherche */}
      <div className="flex-1 sm:max-w-xs">
        <Label htmlFor="mes-factures-recherche" className="mb-2 block">
          Rechercher
        </Label>
        <div className="relative">
          <Input
            id="mes-factures-recherche"
            type="text"
            placeholder="Numéro ou référence..."
            value={recherche ?? ""}
            onChange={(e) => onRechercheChange(e.target.value || undefined)}
            className="pl-10"
          />
          <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
        </div>
      </div>

      {/* Laboratoire */}
      <div className="w-full sm:max-w-xs">
        <Label htmlFor="mes-factures-labo" className="mb-2 block">
          Laboratoire
        </Label>
        <Select
          id="mes-factures-labo"
          value={laboratoireId ?? ""}
          onChange={(e) => {
            const valeur = e.target.value;
            onLaboratoireChange(valeur === "" ? undefined : Number(valeur));
          }}
        >
          <option value="">Tous les laboratoires</option>
          {laboratoires.map((lab) => (
            <option key={lab.id} value={lab.id}>
              {lab.nom}
            </option>
          ))}
        </Select>
      </div>

      {/* Statut */}
      <div className="w-full sm:max-w-xs">
        <Label htmlFor="mes-factures-statut" className="mb-2 block">
          Statut
        </Label>
        <Select
          id="mes-factures-statut"
          value={statut ?? ""}
          onChange={(e) => {
            const valeur = e.target.value;
            onStatutChange(valeur === "" ? undefined : (valeur as StatutFactureClient | "RETARD"));
          }}
        >
          <option value="">Tous les statuts</option>
          <option value="IMPAYEE">Impayée</option>
          <option value="RETARD">En retard</option>
          <option value="PAYEE">Payée</option>
        </Select>
      </div>
    </div>
  );
}
