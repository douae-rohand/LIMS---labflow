import { Link, useNavigate } from "@tanstack/react-router";
import { Skeleton } from "@/components/lab";
import { formatDateFr } from "@/lib/dates";
import { messageErreurRapports, type RapportResumeClient } from "@/api/rapportsClient";
import { libelleTypeRapport } from "./libelles";

interface RapportsClientListeProps {
  rapports: RapportResumeClient[] | undefined;
  isLoading: boolean;
  isError: boolean;
  error: unknown;
  onReessayer: () => void;
}

const COLONNES = 6;

/** Liste « Mes rapports » : lecture seule, une ligne par rapport visible. */
export function RapportsClientListe({
  rapports,
  isLoading,
  isError,
  error,
  onReessayer,
}: RapportsClientListeProps) {
  const navigate = useNavigate();

  const allerAuDetail = (rapport: RapportResumeClient) => {
    void navigate({
      to: "/app/rapports/$laboratoireId/$rapportId",
      params: {
        laboratoireId: String(rapport.laboratoireId),
        rapportId: String(rapport.rapportId),
      },
    });
  };

  return (
    <div className="overflow-x-auto rounded-2xl border border-brand-900/10">
      <table className="w-full min-w-[760px] text-left text-sm">
        <thead className="bg-surface-card text-brand-900">
          <tr>
            <th scope="col" className="w-36 px-4 py-3 font-semibold">
              Demande
            </th>
            <th scope="col" className="px-4 py-3 font-semibold">
              Titre
            </th>
            <th scope="col" className="w-56 px-4 py-3 font-semibold">
              Laboratoire
            </th>
            <th scope="col" className="w-32 px-4 py-3 font-semibold">
              Type
            </th>
            <th scope="col" className="w-24 px-4 py-3 font-semibold">
              Version
            </th>
            <th scope="col" className="w-32 px-4 py-3 font-semibold text-right">
              Émis le
            </th>
          </tr>
        </thead>
        <tbody>
          {isLoading &&
            rapports === undefined &&
            Array.from({ length: 5 }).map((_, i) => (
              <tr key={i} className="border-t border-brand-900/10">
                {Array.from({ length: COLONNES }).map((__, j) => (
                  <td key={j} className="px-4 py-3">
                    <Skeleton className="h-4 w-full" />
                  </td>
                ))}
              </tr>
            ))}

          {isError && (
            <tr>
              <td colSpan={COLONNES} className="px-4 py-8 text-center">
                <p className="mb-3 text-sm text-red-600">{messageErreurRapports(error)}</p>
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

          {!isLoading &&
            !isError &&
            rapports !== undefined &&
            rapports.map((rapport) => (
              <tr
                key={`${rapport.laboratoireId}-${rapport.rapportId}`}
                onClick={() => allerAuDetail(rapport)}
                className="group border-t border-brand-900/10 hover:bg-surface-card/60 cursor-pointer"
              >
                <td className="px-4 py-3 font-mono text-xs text-muted-foreground">
                  <Link
                    to="/app/rapports/$laboratoireId/$rapportId"
                    params={{
                      laboratoireId: String(rapport.laboratoireId),
                      rapportId: String(rapport.rapportId),
                    }}
                    className="hover:text-brand-900 hover:underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-900"
                    onClick={(e) => e.stopPropagation()}
                  >
                    {rapport.demandeReference ?? "Rapport"}
                  </Link>
                </td>
                <td className="px-4 py-3 font-semibold text-ink-900">
                  <Link
                    to="/app/rapports/$laboratoireId/$rapportId"
                    params={{
                      laboratoireId: String(rapport.laboratoireId),
                      rapportId: String(rapport.rapportId),
                    }}
                    className="group-hover:text-brand-900 transition-colors"
                    onClick={(e) => e.stopPropagation()}
                  >
                    {rapport.demandeTitre ?? "—"}
                  </Link>
                </td>
                <td className="px-4 py-3 text-muted-foreground">
                  {rapport.laboratoireNom ?? "—"}
                </td>
                <td className="px-4 py-3 text-muted-foreground">
                  {libelleTypeRapport(rapport.type)}
                </td>
                <td className="px-4 py-3 text-muted-foreground">
                  {rapport.version != null ? `v${rapport.version}` : "—"}
                </td>
                <td className="px-4 py-3 text-muted-foreground text-right">
                  {formatDateFr(rapport.dateEmission)}
                </td>
              </tr>
            ))}

          {!isLoading && !isError && rapports !== undefined && rapports.length === 0 && (
            <tr>
              <td colSpan={COLONNES} className="px-4 py-10 text-center text-sm text-muted-foreground">
                Vous n&apos;avez aucun rapport disponible pour le moment.
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}
