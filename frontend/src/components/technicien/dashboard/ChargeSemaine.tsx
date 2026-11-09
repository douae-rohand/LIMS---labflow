import { Card, Skeleton } from "@/components/lab";
import { formatDateLocale } from "@/components/technicien/libelles";
import type { ChargeSemaineDto } from "@/api/technicien/types";
import { ApiError } from "@/api/axios";

/** Valeur maximale pour normaliser les hauteurs de barre. */
function maxCharge(charge: ChargeSemaineDto[]): number {
  const max = charge.reduce((acc, c) => Math.max(acc, c.count), 0);
  return max > 0 ? max : 1;
}

/**
 * Charge de la semaine en barres CSS accessibles.
 * Chaque barre porte un `role="img"` et un `aria-label` décrivant le jour et le count.
 * Un tableau alternatif (caché visuellement mais accessible) est fourni en repli.
 */
function BarresCharge({ charge }: { charge: ChargeSemaineDto[] }) {
  const max = maxCharge(charge);
  return (
    <div>
      {/* Tableau alternatif accessible (visuellement masqué) */}
      <table className="sr-only">
        <caption>Charge de la semaine (7 prochains jours)</caption>
        <thead>
          <tr>
            <th scope="col">Date</th>
            <th scope="col">Nombre d'essais</th>
          </tr>
        </thead>
        <tbody>
          {charge.map((c) => (
            <tr key={c.date}>
              <th scope="row">{formatDateLocale(c.date)}</th>
              <td>{c.count}</td>
            </tr>
          ))}
        </tbody>
      </table>

      {/* Barres visuelles */}
      <div
        className="flex items-end gap-2 h-32"
        role="img"
        aria-label="Charge de la semaine en barres"
      >
        {charge.map((c) => {
          const hauteur = Math.max((c.count / max) * 100, 4);
          return (
            <div
              key={c.date}
              className="flex-1 flex flex-col items-center justify-end gap-1"
            >
              <span className="text-xs font-bold text-ink-900">{c.count}</span>
              <div
                className="w-full rounded-t-lg bg-brand-900/80 transition-all duration-300"
                style={{ height: `${hauteur}%` }}
                role="img"
                aria-label={`${formatDateLocale(c.date)} : ${c.count} essai(s) attribué(s)`}
              />
              <span className="text-[10px] text-muted-foreground truncate w-full text-center">
                {formatDateLocale(c.date).split(" ").slice(1, 3).join(" ")}
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
}

/** Composant wrapper : charge les données de synthèse et affiche les barres. */
export function ChargeSemaine({
  charge,
  isLoading,
  isError,
  error,
}: {
  charge: ChargeSemaineDto[] | undefined;
  isLoading: boolean;
  isError: boolean;
  error: unknown;
}) {
  if (isLoading) {
    return <Skeleton className="h-32 w-full rounded-3xl" />;
  }

  if (isError) {
    if (error instanceof ApiError && error.status === 403) {
      return (
        <Card variant="flat" padding="md" className="text-center">
          <p className="text-sm text-muted-foreground">Charge indisponible.</p>
        </Card>
      );
    }
    return (
      <Card variant="flat" padding="md" className="text-center">
        <p className="text-sm text-red-700" role="alert">
          Impossible de charger la charge de la semaine.
        </p>
      </Card>
    );
  }

  if (!charge || charge.length === 0) {
    return (
      <Card variant="flat" padding="md" className="text-center">
        <p className="text-sm text-muted-foreground">Aucune charge prévue sur la semaine.</p>
      </Card>
    );
  }

  return (
    <Card variant="surface" padding="md">
      <BarresCharge charge={charge} />
    </Card>
  );
}
