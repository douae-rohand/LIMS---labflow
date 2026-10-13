import { Badge } from "@/components/lab";
import { LIBELLE_STATUT_DEMANDE, type StatutDemande } from "@/api/demandes";

const VARIANT: Record<StatutDemande, "lime" | "mint" | "soft" | "dark" | "outline"> = {
  NOUVEAU: "mint",
  ACCEPTEE: "lime",
  EN_COURS: "soft",
  EN_VALIDATION: "soft",
  TERMINEE: "dark",
  ANNULEE: "outline",
  REFUSEE: "outline",
};

export function StatutDemandeBadge({ statut }: { statut: StatutDemande }) {
  return (
    <Badge variant={VARIANT[statut]} size="sm">
      {LIBELLE_STATUT_DEMANDE[statut] ?? statut}
    </Badge>
  );
}
