import { Clock, FlaskConical } from "lucide-react";
import type { AnalysePublique, ExigencePublique } from "@/api/laboratoires";
import { Badge, Button, Card, IconBox } from "@/components/lab";

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

function detailExigence(exigence: ExigencePublique): string | null {
  const morceaux: string[] = [];
  if (exigence.quantiteMinimale != null && exigence.quantiteMinimale !== "") {
    const quantite =
      typeof exigence.quantiteMinimale === "number"
        ? exigence.quantiteMinimale
        : Number(exigence.quantiteMinimale);
    if (!Number.isNaN(quantite)) {
      morceaux.push(exigence.unite ? `${quantite} ${exigence.unite}` : `${quantite}`);
    }
  } else if (exigence.unite?.trim()) {
    morceaux.push(exigence.unite);
  }
  if (exigence.etatConservation?.trim()) {
    morceaux.push(exigence.etatConservation);
  }
  return morceaux.length > 0 ? morceaux.join(" · ") : null;
}

export function AnalyseCard({
  analyse,
  dejaDansPanier = false,
  enAjout = false,
  onAjouter,
}: {
  analyse: AnalysePublique;
  dejaDansPanier?: boolean;
  enAjout?: boolean;
  onAjouter?: (() => void) | undefined;
}) {
  const tarif = formatTarif(analyse.tarif);
  const duree = formatDuree(analyse.dureeEstimee);
  const exigences = analyse.exigences ?? [];

  return (
    <Card variant="glossy" className="flex h-full flex-col">
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
      </dl>
      {exigences.length > 0 ? (
        <div className="mt-4 rounded-2xl bg-mint-200/40 px-4 py-3">
          <p className="text-xs font-semibold tracking-wide text-brand-900 uppercase">
            Échantillon requis
          </p>
          <ul className="mt-2 flex flex-col gap-1.5 text-sm text-ink-900">
            {exigences.map((exigence, index) => {
              const detail = detailExigence(exigence);
              return (
                <li key={`${exigence.nature ?? "exigence"}-${index}`}>
                  <span className="font-semibold">{exigence.nature ?? "Échantillon"}</span>
                  {detail ? ` — ${detail}` : ""}
                </li>
              );
            })}
          </ul>
        </div>
      ) : null}
      {onAjouter ? (
        <div className="mt-5 flex justify-end">
          <Button
            size="sm"
            variant={dejaDansPanier ? "secondary" : "primary"}
            disabled={dejaDansPanier || enAjout}
            loading={enAjout}
            onClick={onAjouter}
          >
            {dejaDansPanier ? "Dans le panier" : "Ajouter"}
          </Button>
        </div>
      ) : null}
    </Card>
  );
}
