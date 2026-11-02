import { Button } from "@/components/lab";
import {
  AlertDialog,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";

interface ConfirmationChangementTypeDialogProps {
  ouvert: boolean;
  onAnnuler: () => void;
  onConfirmer: () => void;
}

/**
 * Confirmation obligatoire avant de repasser un compte entreprise en compte
 * particulier : la raison sociale et l'ICE enregistrés sont supprimés.
 */
export function ConfirmationChangementTypeDialog({
  ouvert,
  onAnnuler,
  onConfirmer,
}: ConfirmationChangementTypeDialogProps) {
  return (
    <AlertDialog
      open={ouvert}
      onOpenChange={(suivant) => {
        if (!suivant) onAnnuler();
      }}
    >
      <AlertDialogContent className="rounded-3xl border-brand-900/10 bg-card">
        <AlertDialogHeader>
          <AlertDialogTitle className="text-ink-900">
            Passer en compte particulier ?
          </AlertDialogTitle>
          <AlertDialogDescription>
            La raison sociale et l&apos;ICE enregistrés pour ce compte seront supprimés. Cette
            opération est définitive.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter className="gap-2">
          <AlertDialogCancel>Conserver le compte entreprise</AlertDialogCancel>
          <Button type="button" onClick={onConfirmer}>
            Passer en compte particulier
          </Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
