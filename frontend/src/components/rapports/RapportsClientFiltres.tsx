import { Label, Select, Input } from "@/components/lab";
import { Search } from "lucide-react";

interface RapportsClientFiltresProps {
  laboratoireId: number | undefined;
  recherche: string | undefined;
  laboratoires: { id: number; nom: string }[];
  onLaboratoireChange: (valeur: number | undefined) => void;
  onRechercheChange: (valeur: string | undefined) => void;
}

/** Filtres de la liste « Mes rapports » : labo et recherche. */
export function RapportsClientFiltres({
  laboratoireId,
  recherche,
  laboratoires,
  onLaboratoireChange,
  onRechercheChange,
}: RapportsClientFiltresProps) {
  return (
    <div className="flex flex-col gap-4 sm:flex-row sm:items-center">
      {/* Recherche */}
      <div className="flex-1 sm:max-w-md">
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
      <div className="w-full sm:w-64">
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
    </div>
  );
}
