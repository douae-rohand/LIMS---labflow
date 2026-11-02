import type { FactureDetailClient } from "@/api/facturesClient";
import { FactureEntete } from "./FactureEntete";
import { FactureLignes } from "./FactureLignes";
import { FacturePaiements } from "./FacturePaiements";

/** Assemblage du détail d'une facture : en-tête, prestations, paiements. */
export function FactureDetail({ facture }: { facture: FactureDetailClient }) {
  return (
    <div className="flex flex-col gap-8">
      <FactureEntete facture={facture} />

      <section>
        <h2 className="mb-4 text-lg font-extrabold text-ink-900">Détail des prestations</h2>
        <FactureLignes lignes={facture.lignes} montantHt={facture.montantHt} />
      </section>

      <section>
        <h2 className="mb-4 text-lg font-extrabold text-ink-900">Paiements</h2>
        <FacturePaiements facture={facture} />
      </section>
    </div>
  );
}
