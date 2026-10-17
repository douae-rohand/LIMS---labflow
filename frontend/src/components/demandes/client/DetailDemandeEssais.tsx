import { formatMontant } from "@/components/panier/format";
import type { LigneEssaiClient } from "@/api/demandesClient";
import { libelleStatutEssai } from "./libelles";

/** Tableau des essais demandés avec leur statut de réalisation. */
export function DetailDemandeEssais({
  lignes,
  total,
}: {
  lignes: LigneEssaiClient[];
  total: number | string | null;
}) {
  if (lignes.length === 0) {
    return <p className="text-sm text-muted-foreground">Aucun essai rattaché à cette demande.</p>;
  }

  return (
    <div className="overflow-x-auto rounded-2xl border border-brand-900/10">
      <table className="w-full min-w-[640px] text-left text-sm">
        <thead className="bg-surface-card text-brand-900">
          <tr>
            <th scope="col" className="px-4 py-3 font-semibold">Code</th>
            <th scope="col" className="px-4 py-3 font-semibold">Désignation</th>
            <th scope="col" className="px-4 py-3 font-semibold">Domaine</th>
            <th scope="col" className="px-4 py-3 font-semibold">Statut</th>
            <th scope="col" className="px-4 py-3 text-right font-semibold">Tarif</th>
          </tr>
        </thead>
        <tbody>
          {lignes.map((ligne, index) => (
            <tr key={ligne.essaiId ?? index} className="border-t border-brand-900/10">
              <td className="px-4 py-3 font-mono text-xs text-muted-foreground">
                {ligne.code ?? "—"}
              </td>
              <td className="px-4 py-3 font-semibold text-ink-900">
                {ligne.designation ?? "Essai"}
              </td>
              <td className="px-4 py-3 text-muted-foreground">{ligne.domaineLibelle ?? "—"}</td>
              <td className="px-4 py-3 text-muted-foreground">
                {libelleStatutEssai(ligne.statut)}
              </td>
              <td className="px-4 py-3 text-right font-semibold text-ink-900">
                {formatMontant(ligne.tarif)}
              </td>
            </tr>
          ))}
        </tbody>
        <tfoot>
          <tr className="border-t border-brand-900/10 bg-surface-card">
            <td colSpan={4} className="px-4 py-3 text-right font-semibold text-brand-900">
              Total
            </td>
            <td className="px-4 py-3 text-right font-bold text-ink-900">{formatMontant(total)}</td>
          </tr>
        </tfoot>
      </table>
    </div>
  );
}
