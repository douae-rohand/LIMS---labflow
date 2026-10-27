import { Badge, Skeleton } from "@/components/lab";
import { messageErreurCatalogue, type AnalyseCatalogue } from "@/api/catalogue";

function formatPrix(value: AnalyseCatalogue["prixUnitaire"]): string {
  if (value == null || value === "") return "—";
  const amount = typeof value === "number" ? value : Number(value);
  if (Number.isNaN(amount)) return "—";
  return new Intl.NumberFormat("fr-MA", { style: "currency", currency: "MAD" }).format(amount);
}

function formatDuree(minutes: number | null): string {
  if (minutes == null) return "—";
  if (minutes >= 60 && minutes % 60 === 0) return `${minutes / 60} h`;
  if (minutes >= 60) return `${Math.floor(minutes / 60)} h ${minutes % 60} min`;
  return `${minutes} min`;
}

interface CatalogueTableProps {
  analyses: AnalyseCatalogue[] | undefined;
  isLoading: boolean;
  isError: boolean;
  error: unknown;
  filtresActifs: boolean;
  onModifier: (analyse: AnalyseCatalogue) => void;
  onReinitialiser: () => void;
  onReessayer: () => void;
}

export function CatalogueTable({
  analyses,
  isLoading,
  isError,
  error,
  filtresActifs,
  onModifier,
  onReinitialiser,
  onReessayer,
}: CatalogueTableProps) {
  return (
    <div className="overflow-x-auto rounded-2xl border border-brand-900/10">
      <table className="w-full min-w-[860px] text-left text-sm">
        <thead className="bg-surface-card text-brand-900">
          <tr>
            <th scope="col" className="px-4 py-3 font-semibold">Code</th>
            <th scope="col" className="px-4 py-3 font-semibold">Analyse</th>
            <th scope="col" className="px-4 py-3 font-semibold">Domaine</th>
            <th scope="col" className="px-4 py-3 font-semibold">Prix</th>
            <th scope="col" className="px-4 py-3 font-semibold">Durée</th>
            <th scope="col" className="px-4 py-3 font-semibold">Visibilité</th>
          </tr>
        </thead>
        <tbody>
          {isLoading && analyses === undefined &&
            Array.from({ length: 6 }).map((_, i) => (
              <tr key={i} className="border-t border-brand-900/10">
                {Array.from({ length: 6 }).map((__, j) => (
                  <td key={j} className="px-4 py-3">
                    <Skeleton className="h-4 w-full" />
                  </td>
                ))}
              </tr>
            ))}

          {isError && (
            <tr>
              <td colSpan={6} className="px-4 py-8 text-center">
                <p className="mb-3 text-sm text-red-600">{messageErreurCatalogue(error)}</p>
                <button
                  type="button"
                  onClick={onReessayer}
                  className="rounded-full border border-brand-900/20 px-4 py-2 text-sm font-semibold text-brand-900 hover:bg-surface-card"
                >
                  Réessayer
                </button>
              </td>
            </tr>
          )}

          {!isLoading && !isError && analyses !== undefined && analyses.length === 0 && (
            <tr>
              <td colSpan={6} className="px-4 py-10 text-center">
                <p className="text-sm font-semibold text-ink-900">
                  {filtresActifs
                    ? "Aucune analyse ne correspond à ces filtres."
                    : "Votre laboratoire n'a pas encore d'analyse dans son catalogue."}
                </p>
                <p className="mt-1 text-sm text-muted-foreground">
                  {filtresActifs
                    ? "Élargissez la recherche pour revoir les analyses de votre laboratoire."
                    : "Ajoutez une analyse pour la proposer aux clients de ce laboratoire."}
                </p>
                {filtresActifs && (
                  <button
                    type="button"
                    onClick={onReinitialiser}
                    className="mt-3 rounded-full border border-brand-900/20 px-4 py-2 text-sm font-semibold text-brand-900 hover:bg-surface-card"
                  >
                    Réinitialiser les filtres
                  </button>
                )}
              </td>
            </tr>
          )}

          {!isError && analyses !== undefined && analyses.length > 0 &&
            analyses.map((analyse) => (
              <tr
                key={analyse.id}
                onClick={() => onModifier(analyse)}
                className="cursor-pointer border-t border-brand-900/10 transition-colors hover:bg-surface-card/60"
              >
                <td className="px-4 py-3 font-mono text-xs font-semibold tracking-wide text-brand-900 uppercase">
                  {analyse.code}
                </td>
                <td className="px-4 py-3">
                  <p className="font-semibold text-ink-900">{analyse.designation}</p>
                  {analyse.normeReference?.trim() ? (
                    <p className="mt-0.5 text-xs text-muted-foreground">{analyse.normeReference}</p>
                  ) : null}
                </td>
                <td className="px-4 py-3 text-muted-foreground">
                  {analyse.domaineLibelle?.trim() || "—"}
                </td>
                <td className="px-4 py-3 whitespace-nowrap">{formatPrix(analyse.prixUnitaire)}</td>
                <td className="px-4 py-3 whitespace-nowrap">{formatDuree(analyse.dureeEstimee)}</td>
                <td className="px-4 py-3">
                  <Badge size="sm" variant={analyse.actif ? "lime" : "soft"}>
                    {analyse.actif ? "Visible" : "Retirée"}
                  </Badge>
                </td>
              </tr>
            ))}
        </tbody>
      </table>
    </div>
  );
}
