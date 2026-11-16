import { Link } from "@tanstack/react-router";
import { Card, Skeleton } from "@/components/lab";
import { formatDateLocale } from "@/components/technicien/libelles";
import type { ChargeSemaineDto } from "@/api/technicien/types";
import { ApiError } from "@/api/axios";

function debutJour(date: string): string {
  return `${date}T00:00:00Z`;
}

function finJour(date: string): string {
  return `${date}T23:59:59Z`;
}

export function ChargeSemaine({ charge, isLoading, isError, error }: {
  charge: ChargeSemaineDto[] | undefined;
  isLoading: boolean;
  isError: boolean;
  error: unknown;
}) {
  if (isLoading) return <Skeleton className="h-44 w-full rounded-3xl" />;
  if (isError) return <Card variant="flat" padding="md"><p role="alert" className="text-sm text-red-700">{error instanceof ApiError && error.status === 403 ? "Planning indisponible." : "Impossible de charger le planning."}</p></Card>;
  if (!charge?.length) return <Card variant="flat" padding="md"><p className="text-sm text-muted-foreground">Aucune affectation prévue sur la période.</p></Card>;
  const max = Math.max(1, ...charge.map((jour) => jour.count));
  const peak = Math.max(...charge.map((jour) => jour.count));
  return <Card variant="surface" padding="md">
    <p className="mb-3 text-xs text-muted-foreground">Essais regroupés selon leur date prévue, ou leur date d’attribution lorsqu’aucune date prévue n’est renseignée.</p>
    <div className="grid grid-cols-7 gap-2" aria-label="Essais attribués dans les sept prochains jours">
      {charge.map((jour) => {
        const href = { to: "/app/essais" as const, search: { page: 1, debut: debutJour(jour.date), fin: finJour(jour.date) } };
        return <Link key={jour.date} {...href} className="group rounded-xl p-1 text-center hover:bg-brand-900/5 focus-visible:outline-2 focus-visible:outline-brand-900" aria-label={`${formatDateLocale(jour.date)} : ${jour.count} essai(s)`}>
          <span className="block text-[10px] text-muted-foreground">{formatDateLocale(jour.date).split(" ")[0]}</span>
          <span className={`mx-auto mt-2 flex h-20 items-end justify-center rounded-lg bg-brand-900/5 ${jour.count === peak && peak > 0 ? "ring-2 ring-brand-900/30" : ""}`}>
            <span className="w-3/5 rounded-t-md bg-brand-900" style={{ height: `${Math.max(5, jour.count / max * 100)}%` }} aria-hidden="true" />
          </span>
          <span className="mt-1 block text-sm font-bold text-ink-900">{jour.count}</span>
          <span className="block truncate text-[10px] text-muted-foreground">{formatDateLocale(jour.date).split(" ").slice(1).join(" ")}</span>
        </Link>;
      })}
    </div>
    <p className="mt-3 text-xs text-muted-foreground">Jour le plus chargé : {peak} essai(s)</p>
  </Card>;
}
