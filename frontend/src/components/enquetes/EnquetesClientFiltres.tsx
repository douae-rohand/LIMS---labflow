import { Label, Select, Input } from "@/components/lab";
import { Search } from "lucide-react";
import {
  LIBELLE_STATUT_ENQUETE,
  STATUTS_ENQUETE,
  type StatutEnqueteClient,
} from "./libelles";

interface EnquetesClientFiltresProps {
  statut: StatutEnqueteClient | undefined;
  laboratoireId: number | undefined;
  recherche: string | undefined;
  laboratoires: { id: number; nom: string }[];
  onStatutChange: (valeur: StatutEnqueteClient | undefined) => void;
  onLaboratoireChange: (valeur: number | undefined) => void;
  onRechercheChange: (valeur: string | undefined) => void;
}

/** Filtres de la liste « Mes enquêtes » : statut, labo et recherche. */
export function EnquetesClientFiltres({
  statut,
  laboratoireId,
  recherche,
  laboratoires,
  onStatutChange,
  onLaboratoireChange,
  onRechercheChange,
}: EnquetesClientFiltresProps) {
  return (
    <div className="flex flex-col gap-4 sm:flex-row sm:items-end">
      {/* Recherche */}
      <div className="flex-1 sm:max-w-xs">
        <Label htmlFor="mes-enquetes-recherche" className="mb-2 block">
          Rechercher
        </Label>
        <div className="relative">
          <Input
            id="mes-enquetes-recherche"
            type="text"
            placeholder="Référence ou objet..."
            value={recherche ?? ""}
            onChange={(e) => onRechercheChange(e.target.value || undefined)}
            className="pl-10"
          />
          <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
        </div>
      </div>

      {/* Laboratoire */}
      <div className="w-full sm:max-w-xs">
        <Label htmlFor="mes-enquetes-labo" className="mb-2 block">
          Laboratoire
        </Label>
        <Select
          id="mes-enquetes-labo"
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
        <Label htmlFor="mes-enquetes-statut" className="mb-2 block">
          État
        </Label>
        <Select
          id="mes-enquetes-statut"
          value={statut ?? ""}
          onChange={(e) => {
            const valeur = e.target.value;
            onStatutChange(valeur === "" ? undefined : (valeur as StatutEnqueteClient));
          }}
        >
          <option value="">Toutes les enquêtes</option>
          {STATUTS_ENQUETE.map((valeur) => (
            <option key={valeur} value={valeur}>
              {LIBELLE_STATUT_ENQUETE[valeur]}
            </option>
          ))}
        </Select>
      </div>
    </div>
  );
}
