import { formatMontant } from "@/components/panier/format";
import type { FactureLigneClient } from "@/api/facturesClient";

interface FactureLignesProps {
  lignes: FactureLigneClient[];
  montantHt: number | string | null;
}

/** Tableau des prestations facturées, avec le total TTC en pied de tableau. */
export function FactureLignes({ lignes, montantHt }: FactureLignesProps) {
  return (
    <div className="overflow-hidden rounded-2xl border border-brand-900/10">
      <table className="w-full text-left text-sm">
        <thead className="bg-surface-card text-brand-900">
          <tr>
            <th scope="col" className="px-4 py-3 font-semibold">
              Prestation
            </th>
            <th scope="col" className="px-4 py-3 text-right font-semibold">
              Montant
            </th>
          </tr>
        </thead>
        <tbody>
          {lignes.length === 0 ? (
            <tr>
              <td colSpan={2} className="px-4 py-6 text-center text-sm text-muted-foreground">
                Aucune ligne détaillée pour cette facture.
              </td>
            </tr>
          ) : (
            lignes.map((ligne, index) => (
              <tr key={index} className="border-t border-brand-900/10">
                <td className="px-4 py-3 text-ink-900">{ligne.designation ?? "—"}</td>
                <td className="px-4 py-3 text-right text-ink-900">
                  {formatMontant(ligne.montant)}
                </td>
              </tr>
            ))
          )}
        </tbody>
        <tfoot>
          <tr className="border-t border-brand-900/10 bg-surface-card">
            <td className="px-4 py-3 font-semibold text-ink-900">Total TTC</td>
            <td className="px-4 py-3 text-right font-bold text-ink-900">
              {formatMontant(montantHt)}
            </td>
          </tr>
        </tfoot>
      </table>
    </div>
  );
}
