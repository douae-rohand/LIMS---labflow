import { Clock, FlaskConical } from "lucide-react";
import type { AnalysePublique } from "@/api/laboratoires";
import { Badge, Card, IconBox } from "@/components/lab";

function formatTarif(value: AnalysePublique["tarif"]): string | null {
  if (value == null || value === "") return null;
  const amount = typeof value === "number" ? value : Number(value);
  if (Number.isNaN(amount)) return null;
  return new Intl.NumberFormat("fr-MA", {
    style: "currency",
    currency: "MAD",
  }).format(amount);
}

function formatDuree(minutes: number | null | undefined): string | null {
  if (minutes == null) return null;
  if (minutes >= 60 && minutes % 60 === 0) return `${minutes / 60} h`;
  if (minutes >= 60) return `${Math.floor(minutes / 60)} h ${minutes % 60} min`;
  return `${minutes} min`;
}

export function AnalyseCard({ analyse }: { analyse: AnalysePublique }) {
  const tarif = formatTarif(analyse.tarif);
  const duree = formatDuree(analyse.dureeEstimee);

  return (
    <Card variant="glossy" className="h-full">
      <div className="flex items-start justify-between gap-3">
        <IconBox icon={FlaskConical} variant="mint" />
        {analyse.domaineLibelle?.trim() ? (
          <Badge size="sm" variant="lime">
            {analyse.domaineLibelle}
          </Badge>
        ) : null}
      </div>
      <h3 className="mt-4 text-lg font-bold text-ink-900">{analyse.designation}</h3>
      <p className="mt-1 text-xs font-semibold tracking-wide text-brand-600 uppercase">
        {analyse.code}
      </p>
      {analyse.description?.trim() ? (
        <p className="mt-3 text-sm text-muted-foreground">{analyse.description}</p>
      ) : null}
      {analyse.methode?.trim() ? (
        <p className="mt-2 text-sm text-muted-foreground">Méthode : {analyse.methode}</p>
      ) : null}
      <dl className="mt-4 flex flex-wrap gap-x-4 gap-y-2 text-sm">
        {tarif ? (
          <div>
            <dt className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
              Tarif
            </dt>
            <dd className="font-bold text-ink-900">{tarif}</dd>
          </div>
        ) : null}
        {duree ? (
          <div>
            <dt className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
              Délai
            </dt>
            <dd className="inline-flex items-center gap-1 font-semibold text-ink-900">
              <Clock className="size-3.5 text-brand-600" aria-hidden="true" />
              {duree}
            </dd>
          </div>
        ) : null}
        {analyse.unite?.trim() ? (
          <div>
            <dt className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
              Unité
            </dt>
            <dd className="font-semibold text-ink-900">{analyse.unite}</dd>
          </div>
        ) : null}
      </dl>
    </Card>
  );
}
