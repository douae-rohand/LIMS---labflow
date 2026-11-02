import { Label, Select, Input } from "@/components/lab";
import { Search } from "lucide-react";

interface RapportsClientFiltresProps {
  type: string | undefined;
  laboratoireId: number | undefined;
  recherche: string | undefined;
  laboratoires: { id: number; nom: string }[];
  onTypeChange: (valeur: string | undefined) => void;
  onLaboratoireChange: (valeur: number | undefined) => void;
  onRechercheChange: (valeur: string | undefined) => void;
}

/** Filtres de la liste « Mes rapports » : type, labo et recherche. */
export function RapportsClientFiltres({
  type,
  laboratoireId,
  recherche,
  laboratoires,
  onTypeChange,
  onLaboratoireChange,
  onRechercheChange,
}: RapportsClientFiltresProps) {
  return (
    <div className="flex flex-col gap-4 sm:flex-row sm:items-end">
      {/* Recherche */}
      <div className="flex-1 sm:max-w-xs">
        <Label htmlFor="mes-rapports-recherche" className="mb-2 block">
          Rechercher
        </Label>
        <div className="relative">
          <Input
            id="mes-rapports-recherche"
            type="text"
            placeholder="Référence ou titre..."
            value={recherche ?? ""}
            onChange={(e) => onRechercheChange(e.target.value || undefined)}
            className="pl-10"
          />
          <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
        </div>
      </div>

      {/* Laboratoire */}
      <div className="w-full sm:max-w-xs">
        <Label htmlFor="mes-rapports-labo" className="mb-2 block">
          Laboratoire
        </Label>
        <Select
          id="mes-rapports-labo"
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

      {/* Type */}
      <div className="w-full sm:max-w-xs">
        <Label htmlFor="mes-rapports-type" className="mb-2 block">
          Type
        </Label>
        <Select
          id="mes-rapports-type"
          value={type ?? ""}
          onChange={(e) => {
            const valeur = e.target.value;
            onTypeChange(valeur === "" ? undefined : valeur);
          }}
        >
          <option value="">Tous les types</option>
          <option value="FINAL">Final</option>
          <option value="PARTIEL">Partiel</option>
          <option value="INTERMEDIAIRE">Intermédiaire</option>
        </Select>
      </div>
    </div>
  );
}
