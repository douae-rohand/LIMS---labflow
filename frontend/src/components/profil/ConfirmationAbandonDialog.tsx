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

interface ConfirmationAbandonDialogProps {
  ouvert: boolean;
  /** L'utilisateur choisit de rester : la navigation est annulée. */
  onRester: () => void;
  /** L'utilisateur confirme l'abandon : la navigation reprend. */
  onAbandonner: () => void;
}

/** Confirmation affichée lorsqu'une navigation interromprait une saisie non enregistrée. */
export function ConfirmationAbandonDialog({
  ouvert,
  onRester,
  onAbandonner,
}: ConfirmationAbandonDialogProps) {
  return (
    <AlertDialog
      open={ouvert}
      onOpenChange={(suivant) => {
        if (!suivant) onRester();
      }}
    >
      <AlertDialogContent className="rounded-3xl border-brand-900/10 bg-card">
        <AlertDialogHeader>
          <AlertDialogTitle className="text-ink-900">
            Quitter sans enregistrer ?
          </AlertDialogTitle>
          <AlertDialogDescription>
            Les modifications apportées à votre profil n&apos;ont pas été enregistrées. Elles
            seront perdues si vous quittez cette page.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter className="gap-2">
          <AlertDialogCancel>Rester sur la page</AlertDialogCancel>
          <Button type="button" onClick={onAbandonner}>
            Quitter sans enregistrer
          </Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
