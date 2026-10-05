import { Link } from "@tanstack/react-router";
import { ArrowRight, Building2, MapPin } from "lucide-react";
import type { LaboratoirePublic } from "@/api/landing";
import { Badge, Card, IconBox } from "@/components/lab";

export function LaboratoireCard({ laboratoire }: { laboratoire: LaboratoirePublic }) {
  return (
    <Link
      to="/laboratoires/$id"
      params={{ id: String(laboratoire.id) }}
      className="block h-full"
    >
      <Card variant="glossy" className="hover-lift h-full">
        <div className="flex items-start justify-between gap-3">
          <IconBox icon={Building2} variant="brand" />
          <Badge size="sm" variant="mint">
            {laboratoire.statut}
          </Badge>
        </div>
        <h3 className="mt-4 text-lg font-bold text-ink-900">
          {laboratoire.raisonSociale}
        </h3>
        <p className="mt-1 text-xs font-semibold tracking-wide text-brand-600 uppercase">
          {laboratoire.code}
        </p>
        <p className="mt-3 flex items-start gap-2 text-sm text-muted-foreground">
          <MapPin className="mt-0.5 size-4 shrink-0 text-brand-600" aria-hidden="true" />
          {laboratoire.ville?.trim() ? laboratoire.ville : "Ville non renseignée"}
        </p>
        {laboratoire.adresse?.trim() ? (
          <p className="mt-1 text-sm text-muted-foreground">{laboratoire.adresse}</p>
        ) : null}
        <p className="mt-4 inline-flex items-center gap-1 text-sm font-semibold text-brand-900">
          Voir la fiche
          <ArrowRight className="size-4" aria-hidden="true" />
        </p>
      </Card>
    </Link>
  );
}
