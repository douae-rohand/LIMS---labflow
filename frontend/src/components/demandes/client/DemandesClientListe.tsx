import { Link } from "@tanstack/react-router";
import { Skeleton } from "@/components/lab";
import { StatutDemandeBadge } from "@/components/demandes/StatutDemandeBadge";
import { formatMontant } from "@/components/panier/format";
import { messageErreurListeClient, type DemandeResumeClient } from "@/api/demandesClient";
import { formatDate } from "./libelles";

interface DemandesClientListeProps {
  demandes: DemandeResumeClient[] | undefined;
  isLoading: boolean;
  isError: boolean;
  error: unknown;
  filtresActifs: boolean;
  onReinitialiser: () => void;
  onReessayer: () => void;
}

const COLONNES = 6;

export function DemandesClientListe({
  demandes,
  isLoading,
  isError,
  error,
  filtresActifs,
  onReinitialiser,
  onReessayer,
}: DemandesClientListeProps) {
  return (
    <div className="overflow-x-auto rounded-2xl border border-brand-900/10">
      <table className="w-full min-w-[760px] text-left text-sm">
        <thead className="bg-surface-card text-brand-900">
          <tr>
            <th scope="col" className="px-4 py-3 font-semibold">Référence</th>
            <th scope="col" className="px-4 py-3 font-semibold">Objet</th>
            <th scope="col" className="px-4 py-3 font-semibold">Laboratoire</th>
            <th scope="col" className="px-4 py-3 font-semibold">Statut</th>
            <th scope="col" className="px-4 py-3 font-semibold">Total</th>
            <th scope="col" className="px-4 py-3 font-semibold">Soumise le</th>
          </tr>
        </thead>
        <tbody>
          {isLoading && demandes === undefined &&
            Array.from({ length: 6 }).map((_, i) => (
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
                <p className="mb-3 text-sm text-red-600">{messageErreurListeClient(error)}</p>
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

          {!isLoading && !isError && demandes !== undefined &&
            demandes.map((demande) => (
              <tr key={demande.id} className="border-t border-brand-900/10 hover:bg-surface-card/60">
                <td className="px-4 py-3 font-mono text-xs text-muted-foreground">
                  {demande.reference}
                </td>
                <td className="px-4 py-3 font-semibold text-ink-900">
                  {demande.laboratoireId !== null ? (
                    <Link
                      to="/app/demandes/$laboratoireId/$demandeId"
                      params={{
                        laboratoireId: String(demande.laboratoireId),
                        demandeId: String(demande.id),
                      }}
                      className="hover:text-brand-900 hover:underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-900"
                    >
                      {demande.objet ?? "Demande"}
                    </Link>
                  ) : (
                    (demande.objet ?? "Demande")
                  )}
                </td>
                <td className="px-4 py-3 text-muted-foreground">
                  {demande.laboratoireNom ?? demande.laboratoireCode ?? "—"}
                </td>
                <td className="px-4 py-3">
                  <StatutDemandeBadge statut={demande.statut} />
                </td>
                <td className="px-4 py-3 font-semibold text-ink-900">
                  {formatMontant(demande.total)}
                </td>
                <td className="px-4 py-3 text-muted-foreground">
                  {formatDate(demande.dateSoumission)}
                </td>
              </tr>
            ))}

          {!isLoading && !isError && demandes !== undefined && demandes.length === 0 && (
            <tr>
              <td
                colSpan={COLONNES}
                className="px-4 py-10 text-center text-sm text-muted-foreground"
              >
                {filtresActifs ? (
                  <>
                    <p>Aucune demande ne correspond à vos filtres.</p>
                    <button
                      type="button"
                      onClick={onReinitialiser}
                      className="mt-3 rounded-full border border-brand-900/20 px-4 py-2 text-sm font-semibold text-brand-900 hover:bg-surface-card"
                    >
                      Réinitialiser les filtres
                    </button>
                  </>
                ) : (
                  <p>Vous n&apos;avez encore envoyé aucune demande.</p>
                )}
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}
