import { Button } from "@/components/lab";
import { formatMontant } from "@/components/panier/format";

export function FactureActions({ factureId, resteAPayer }: { factureId: number; resteAPayer: number }) {
  return (
    <div className="flex gap-2">
      <Button variant="primary" size="sm">
        Émettre une facture
      </Button>
      {resteAPayer > 0 && (
        <Button variant="lime" size="sm">
          Enregistrer le paiement ({formatMontant(resteAPayer)} MAD)
        </Button>
      )}
    </div>
  );
}
