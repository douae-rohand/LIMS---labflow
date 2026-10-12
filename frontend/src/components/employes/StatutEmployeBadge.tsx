import { Badge } from "@/components/lab";
import type { StatutEmploye } from "@/api/employes";

const LIBELLE_STATUT: Record<StatutEmploye, string> = {
  ACTIF: "Actif",
  EN_ATTENTE: "En attente",
  DESACTIVE: "Désactivé",
};

const VARIANT_STATUT: Record<StatutEmploye, "lime" | "mint" | "outline"> = {
  ACTIF: "lime",
  EN_ATTENTE: "mint",
  DESACTIVE: "outline",
};

export function StatutEmployeBadge({ statut }: { statut: StatutEmploye }) {
  return (
    <Badge variant={VARIANT_STATUT[statut]} size="sm">
      {LIBELLE_STATUT[statut]}
    </Badge>
  );
}
