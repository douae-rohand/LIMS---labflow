import { Link, useNavigate } from "@tanstack/react-router";
import { Badge, Skeleton } from "@/components/lab";
import { formatDateFr } from "@/lib/dates";
import { messageErreurEnquetes, type EnqueteResumeClient } from "@/api/enquetesClient";
import {
  libelleNote,
  libelleStatutEnquete,
  variantStatutEnquete,
} from "./libelles";

interface EnquetesClientListeProps {
  enquetes: EnqueteResumeClient[] | undefined;
  isLoading: boolean;
  isError: boolean;
  error: unknown;
  onReessayer: () => void;
}

const COLONNES = 6;

/** Liste « Mes enquêtes » : une ligne par enquête, action selon l'état. */
export function EnquetesClientListe({
  enquetes,
  isLoading,
  isError,
  error,
  onReessayer,
}: EnquetesClientListeProps) {
  const navigate = useNavigate();

  const allerAuDetail = (enquete: EnqueteResumeClient) => {
    void navigate({
      to: "/app/enquetes/$laboratoireId/$enqueteId",
      params: {
        laboratoireId: String(enquete.laboratoireId),
        enqueteId: String(enquete.enqueteId),
      },
    });
  };

  return (
    <div className="overflow-x-auto rounded-2xl border border-brand-900/10">
      <table className="w-full min-w-[760px] text-left text-sm">
        <thead className="bg-surface-card text-brand-900">
          <tr>
            <th scope="col" className="w-36 px-4 py-3 font-semibold">
              Référence
            </th>
            <th scope="col" className="px-4 py-3 font-semibold">
              Demande
            </th>
            <th scope="col" className="w-36 px-4 py-3 font-semibold">
              Envoyée le
            </th>
            <th scope="col" className="w-36 px-4 py-3 font-semibold">
              État
            </th>
            <th scope="col" className="w-40 px-4 py-3 font-semibold">
              Note globale
            </th>
            <th scope="col" className="w-28 px-4 py-3 font-semibold text-right">
              Action
            </th>
          </tr>
        </thead>
        <tbody>
          {isLoading &&
            enquetes === undefined &&
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
                <p className="mb-3 text-sm text-red-600">{messageErreurEnquetes(error)}</p>
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
            enquetes !== undefined &&
            enquetes.map((enquete) => (
              <tr
                key={`${enquete.laboratoireId}-${enquete.enqueteId}`}
                onClick={() => allerAuDetail(enquete)}
                className="group border-t border-brand-900/10 hover:bg-surface-card/60 cursor-pointer"
              >
                <td className="px-4 py-3 font-mono text-xs text-muted-foreground">
                  {enquete.demandeReference ?? "—"}
                </td>
                <td className="px-4 py-3 font-semibold text-ink-900">
                  {enquete.demandeTitre ?? enquete.laboratoireNom ?? "-"}
                </td>
                <td className="px-4 py-3 text-muted-foreground">
                  {formatDateFr(enquete.dateEnvoi)}
                </td>
                <td className="px-4 py-3">
                  <Badge variant={variantStatutEnquete(enquete.repondue)} size="sm">
                    {libelleStatutEnquete(enquete.repondue)}
                  </Badge>
                </td>
                <td className="px-4 py-3 text-muted-foreground">
                  {libelleNote(enquete.noteGlobale)}
                </td>
                <td className="px-4 py-3 text-right">
                  <Link
                    to="/app/enquetes/$laboratoireId/$enqueteId"
                    params={{
                      laboratoireId: String(enquete.laboratoireId),
                      enqueteId: String(enquete.enqueteId),
                    }}
                    className="font-semibold text-brand-900 hover:underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-900"
                    onClick={(e) => e.stopPropagation()}
                  >
                    {enquete.repondue ? "Consulter" : "Répondre"}
                  </Link>
                </td>
              </tr>
            ))}

          {!isLoading && !isError && enquetes !== undefined && enquetes.length === 0 && (
            <tr>
              <td colSpan={COLONNES} className="px-4 py-10 text-center text-sm text-muted-foreground">
                Aucune enquête ne correspond à votre filtre.
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}
