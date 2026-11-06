import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { useNavigate } from "@tanstack/react-router";
import { AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle, AlertDialogTrigger } from "@/components/ui/alert-dialog";
import { Button } from "@/components/lab";
import { messageErreurFacturesAdmin } from "@/api/facturesAdmin";
import { formatMontant } from "@/components/panier/format";

interface EnregistrerPaiementDialogProps {
  factureId: number;
  resteAPayer: number;
  numero: string;
  onSuccess?: (() => void) | undefined;
}

export function EnregistrerPaiementDialog({
  factureId,
  resteAPayer,
  numero,
  onSuccess,
}: EnregistrerPaiementDialogProps) {
  const [open, setOpen] = useState(false);
  const [mode, setMode] = useState<"ESPECES" | "VIREMENT" | "CHEQUE" | "CARTE">("VIREMENT");
  const [reference, setReference] = useState("");
  const navigate = useNavigate();

  const enregistrer = useMutation({
    mutationFn: () => import("@/api/facturesAdmin").then(m => m.enregistrerPaiement(factureId, { mode, reference: reference || undefined })),
    onSuccess: () => {
      setOpen(false);
      if (onSuccess) {
        onSuccess();
      } else {
        navigate({ to: "/app/facturation" });
      }
    },
  });

  return (
    <AlertDialog open={open} onOpenChange={setOpen}>
      <AlertDialogTrigger asChild>
        <Button variant="lime" size="sm">
          Enregistrer le paiement ({formatMontant(resteAPayer)})
        </Button>
      </AlertDialogTrigger>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Enregistrer un paiement</AlertDialogTitle>
          <AlertDialogDescription>
            Facture {numero} - Reste à payer : {formatMontant(resteAPayer)}
          </AlertDialogDescription>
        </AlertDialogHeader>
        <div className="space-y-4 py-4">
          <div>
            <label htmlFor="mode" className="block text-sm font-medium mb-2">Mode de paiement</label>
            <select
              id="mode"
              value={mode}
              onChange={(e) => setMode(e.target.value as any)}
              className="w-full px-3 py-2 rounded-md border border-brand-900/20"
            >
              <option value="ESPECES">Espèces</option>
              <option value="VIREMENT">Virement</option>
              <option value="CHEQUE">Chèque</option>
              <option value="CARTE">Carte</option>
            </select>
          </div>
          <div>
            <label htmlFor="reference" className="block text-sm font-medium mb-2">Référence (optionnelle)</label>
            <input
              id="reference"
              type="text"
              value={reference}
              onChange={(e) => setReference(e.target.value)}
              placeholder="Numéro de chèque, référence de virement..."
              className="w-full px-3 py-2 rounded-md border border-brand-900/20"
            />
          </div>
        </div>
        <AlertDialogFooter>
          <AlertDialogCancel asChild>
            <Button variant="ghost" size="sm">Annuler</Button>
          </AlertDialogCancel>
          <AlertDialogAction asChild>
            <Button
              variant="primary"
              size="sm"
              onClick={() => enregistrer.mutate()}
              disabled={enregistrer.isPending}
            >
              {enregistrer.isPending ? "Enregistrement..." : "Confirmer"}
            </Button>
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
