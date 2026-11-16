import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect } from "react";
import { useController, useForm } from "react-hook-form";
import { z } from "zod";
import { Button, Label, Textarea } from "@/components/lab";
import {
  AlertDialog,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { useDeclareNonRealisable } from "@/hooks/useTechnicienEssais";

const schema = z.object({
  motif: z
    .string()
    .trim()
    .min(5, "Le motif doit contenir au moins 5 caractères.")
    .max(1000, "Le motif ne peut pas dépasser 1000 caractères."),
});

type Valeurs = z.infer<typeof schema>;

/** Dialogue de déclaration d'essai non réalisable, avec motif obligatoire. */
export function DialogueNonRealisable({
  id,
  open,
  onOpenChange,
}: {
  id: number;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const {
    control,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<Valeurs>({
    resolver: zodResolver(schema),
    defaultValues: { motif: "" },
  });

  const motifField = useController({ name: "motif", control });
  const mutation = useDeclareNonRealisable();
  const enCours = mutation.isPending;

  useEffect(() => {
    if (open) reset({ motif: "" });
  }, [open, reset]);

  const confirmer = (valeurs: Valeurs) => {
    mutation.mutate(
      { id, payload: { motif: valeurs.motif } },
      { onSuccess: () => onOpenChange(false) },
    );
  };

  return (
    <AlertDialog open={open} onOpenChange={(next) => !enCours && onOpenChange(next)}>
      <AlertDialogContent className="rounded-3xl border-brand-900/10 bg-card">
        <AlertDialogHeader>
          <AlertDialogTitle className="text-ink-900">
            Marquer cet essai non réalisable ?
          </AlertDialogTitle>
          <AlertDialogDescription>
            Indiquez le motif. L&apos;essai passera en statut « Non réalisable » et ne pourra
            plus être repris.
          </AlertDialogDescription>
        </AlertDialogHeader>

        <form className="space-y-4" noValidate onSubmit={handleSubmit(confirmer)}>
          <div className="space-y-2">
            <Label htmlFor="motif-non-realisable">Motif</Label>
            <Textarea
              id="motif-non-realisable"
              rows={4}
              maxLength={1000}
              placeholder="Ex. : échantillon insuffisant pour réaliser l'analyse"
              disabled={enCours}
              value={motifField.field.value}
              onChange={motifField.field.onChange}
              onBlur={motifField.field.onBlur}
              aria-invalid={errors.motif ? true : undefined}
              aria-describedby={errors.motif ? "motif-erreur" : undefined}
            />
            {errors.motif ? (
              <p id="motif-erreur" role="alert" className="text-sm text-red-600">
                {errors.motif.message}
              </p>
            ) : null}
          </div>

          <AlertDialogFooter className="gap-2">
            <AlertDialogCancel disabled={enCours}>Conserver l&apos;essai</AlertDialogCancel>
            <Button type="submit" loading={enCours} disabled={enCours}>
              Confirmer
            </Button>
          </AlertDialogFooter>
        </form>
      </AlertDialogContent>
    </AlertDialog>
  );
}
