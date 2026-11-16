import { useState } from "react";
import { ShieldCheck } from "lucide-react";
import { Button, Card, Label, Textarea } from "@/components/lab";
import {
  AlertDialog,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { useValiderTechniquement } from "@/hooks/useTechnicienEssais";
import type { LigneEssaiDetailTechnicienDto } from "@/api/technicien/types";

/**
 * Bloc validation technique (niveau 1).
 * Le technicien ne peut que VALIDER. Une confirmation par alert-dialog rappelle
 * que c'est la validation de premier niveau.
 */
export function BlocValidationTechnique({ detail }: { detail: LigneEssaiDetailTechnicienDto }) {
  const [open, setOpen] = useState(false);
  const [commentaire, setCommentaire] = useState("");
  const mutation = useValiderTechniquement();
  const enCours = mutation.isPending;

  const confirmer = () => {
    const payload = commentaire.trim() === "" ? {} : { commentaire: commentaire.trim() };
    mutation.mutate(
      { id: detail.ligneEssaiId, payload },
      { onSuccess: () => {
        setOpen(false);
        setCommentaire("");
      } },
    );
  };

  return (
    <Card variant="flat" padding="md" className="space-y-4">
      <div className="flex items-center gap-2">
        <ShieldCheck className="size-5 text-brand-900" aria-hidden="true" />
        <h2 className="text-lg font-bold text-ink-900">Validation technique</h2>
      </div>

      <p className="text-sm text-muted-foreground">
        En validant, vous confirmez que le résultat saisi est correct. Cette validation est
        de premier niveau : le responsable validera ensuite.
      </p>

      <Button onClick={() => setOpen(true)} icon={<ShieldCheck className="size-4" aria-hidden="true" />} iconPosition="left">
        Valider techniquement
      </Button>

      <AlertDialog open={open} onOpenChange={(next) => !enCours && setOpen(next)}>
        <AlertDialogContent className="rounded-3xl border-brand-900/10 bg-card">
          <AlertDialogHeader>
            <AlertDialogTitle className="text-ink-900">Valider techniquement cet essai ?</AlertDialogTitle>
            <AlertDialogDescription>
              Cette action enregistre la validation technique (niveau 1). Le résultat ne pourra
              plus être modifié après validation par le responsable.
            </AlertDialogDescription>
          </AlertDialogHeader>

          <div className="space-y-2">
            <Label htmlFor="commentaire-validation">Commentaire (facultatif)</Label>
            <Textarea
              id="commentaire-validation"
              rows={3}
              maxLength={2000}
              placeholder="Précisez un contexte si utile."
              disabled={enCours}
              value={commentaire}
              onChange={(e) => setCommentaire(e.target.value)}
            />
          </div>

          <AlertDialogFooter className="gap-2">
            <AlertDialogCancel disabled={enCours}>Annuler</AlertDialogCancel>
            <Button type="button" loading={enCours} disabled={enCours} onClick={confirmer}>
              Confirmer la validation
            </Button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </Card>
  );
}
