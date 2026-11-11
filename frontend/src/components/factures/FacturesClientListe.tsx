import { Link, useNavigate } from "@tanstack/react-router";
import { Badge, Skeleton } from "@/components/lab";
import { formatMontant } from "@/components/panier/format";
import { formatDateLocaleFr, echeanceDepassee } from "@/lib/dates";
import { messageErreurFactures, type FactureResumeClient } from "@/api/facturesClient";
import { libelleStatutFacture, variantStatutFacture } from "./libelles";

interface FacturesClientListeProps {
  factures: FactureResumeClient[] | undefined;
  isLoading: boolean;
  isError: boolean;
  error: unknown;
  onReessayer: () => void;
}

const COLONNES = 8;

export function FacturesClientListe({
  factures,
  isLoading,
  isError,
  error,
  onReessayer,
}: FacturesClientListeProps) {
  const navigate = useNavigate();

  const allerAuDetail = (facture: FactureResumeClient) => {
    void navigate({
      to: "/app/factures/$laboratoireId/$factureId",
      params: {
        laboratoireId: String(facture.laboratoireId),
        factureId: String(facture.factureId),
      },
    });
  };

  return (
    <div className="overflow-x-auto rounded-2xl border border-brand-900/10">
      <table className="w-full min-w-237.5 text-left text-sm">
        <thead className="bg-surface-card text-brand-900">
          <tr>
            <th scope="col" className="w-32 px-4 py-3 font-semibold">Numéro</th>
            <th scope="col" className="w-36 px-4 py-3 font-semibold">Référence demande</th>
            <th scope="col" className="w-32 px-4 py-3 font-semibold">Émise le</th>
            <th scope="col" className="w-32 px-4 py-3 font-semibold">Échéance</th>
            <th scope="col" className="w-28 px-4 py-3 font-semibold text-right">Montant</th>
            <th scope="col" className="w-28 px-4 py-3 font-semibold text-right">Payé</th>
            <th scope="col" className="w-28 px-4 py-3 font-semibold text-right">Reste dû</th>
            <th scope="col" className="w-36 px-4 py-3 font-semibold text-right">Statut</th>
          </tr>
        </thead>
        <tbody>
          {isLoading && factures === undefined &&
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
                <p className="mb-3 text-sm text-red-600">{messageErreurFactures(error)}</p>
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

          {!isLoading && !isError && factures !== undefined &&
            factures.map((facture) => {
              const enRetard = facture.statut === "IMPAYEE" && echeanceDepassee(facture.dateEcheance);
              return (
                <tr
                  key={`${facture.laboratoireId}-${facture.factureId}`}
                  onClick={() => allerAuDetail(facture)}
                  className="group border-t border-brand-900/10 hover:bg-surface-card/60 cursor-pointer"
                >
                  <td className="px-4 py-3 font-semibold text-ink-900">
                    <Link
                      to="/app/factures/$laboratoireId/$factureId"
                      params={{
                        laboratoireId: String(facture.laboratoireId),
                        factureId: String(facture.factureId),
                      }}
                      className="font-mono text-xs hover:text-brand-900 hover:underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-900"
                      onClick={(e) => e.stopPropagation()}
                    >
                      {facture.numero}
                    </Link>
                  </td>
                  <td className="px-4 py-3 font-mono text-xs text-muted-foreground">
                    {facture.demandeReference ?? "—"}
                  </td>
                  <td className="px-4 py-3 text-muted-foreground">
                    {formatDateLocaleFr(facture.dateEmission)}
                  </td>
                  <td className="px-4 py-3 text-muted-foreground">
                    {formatDateLocaleFr(facture.dateEcheance)}
                  </td>
                  <td className="px-4 py-3 text-ink-900 text-right">{formatMontant(facture.montantTotal)}</td>
                  <td className="px-4 py-3 text-muted-foreground text-right">
                    {formatMontant(facture.montantPaye)}
                  </td>
                  <td className="px-4 py-3 font-semibold text-ink-900 text-right">
                    {formatMontant(facture.resteAPayer)}
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex flex-wrap items-center justify-end gap-2">
                      <Badge variant={variantStatutFacture(facture.statut)} size="sm">
                        {libelleStatutFacture(facture.statut)}
                      </Badge>
                      {enRetard ? (
                        <Badge variant="outline" size="sm">
                          En retard
                        </Badge>
                      ) : null}
                    </div>
                  </td>
                </tr>
              );
            })}

          {!isLoading && !isError && factures !== undefined && factures.length === 0 && (
            <tr>
              <td
                colSpan={COLONNES}
                className="px-4 py-10 text-center text-sm text-muted-foreground"
              >
                Aucune facture ne correspond à votre recherche.
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}
