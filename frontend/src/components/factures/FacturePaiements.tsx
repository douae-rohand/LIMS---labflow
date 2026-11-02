import { formatMontant } from "@/components/panier/format";
import { formatDateFr } from "@/lib/dates";
import type { FactureDetailClient } from "@/api/facturesClient";

/** Historique des paiements enregistrés, suivi du rappel des montants clés. */
export function FacturePaiements({ facture }: { facture: FactureDetailClient }) {
  return (
    <div className="flex flex-col gap-4">
      <div className="overflow-hidden rounded-2xl border border-brand-900/10">
        <table className="w-full text-left text-sm">
          <thead className="bg-surface-card text-brand-900">
            <tr>
              <th scope="col" className="px-4 py-3 font-semibold">
                Date
              </th>
              <th scope="col" className="px-4 py-3 font-semibold">
                Mode
              </th>
              <th scope="col" className="px-4 py-3 text-right font-semibold">
                Montant
              </th>
            </tr>
          </thead>
          <tbody>
            {facture.paiements.length === 0 ? (
              <tr>
                <td colSpan={3} className="px-4 py-6 text-center text-sm text-muted-foreground">
                  Aucun paiement enregistré.
                </td>
              </tr>
            ) : (
              facture.paiements.map((paiement, index) => (
                <tr key={index} className="border-t border-brand-900/10">
                  <td className="px-4 py-3 text-muted-foreground">
                    {formatDateFr(paiement.date)}
                  </td>
                  <td className="px-4 py-3 text-muted-foreground">{paiement.mode ?? "—"}</td>
                  <td className="px-4 py-3 text-right text-ink-900">
                    {formatMontant(paiement.montant)}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <dl className="grid gap-4 sm:grid-cols-3">
        <div>
          <dt className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
            Montant HT
          </dt>
          <dd className="mt-1 text-sm font-semibold text-ink-900">
            {formatMontant(facture.montantHt)}
          </dd>
        </div>
        <div>
          <dt className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
            Total payé
          </dt>
          <dd className="mt-1 text-sm font-semibold text-ink-900">
            {formatMontant(facture.montantPaye)}
          </dd>
        </div>
        <div>
          <dt className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
            Reste à payer
          </dt>
          <dd className="mt-1 text-sm font-bold text-ink-900">
            {formatMontant(facture.resteAPayer)}
          </dd>
        </div>
      </dl>
    </div>
  );
}
