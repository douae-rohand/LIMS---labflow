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

const schema = z.object({
  motif: z
    .string()
    .trim()
    .min(5, "Le motif doit contenir au moins 5 caractères.")
    .max(2000, "Le motif ne peut pas dépasser 2000 caractères."),
});

type Valeurs = z.infer<typeof schema>;

interface AnnulerDemandeDialogProps {
  open: boolean;
  chargement: boolean;
  onOpenChange: (open: boolean) => void;
  onConfirmer: (motif: string) => void;
}

/** Confirmation d'annulation d'une demande, avec motif obligatoire. */
export function AnnulerDemandeDialog({
  open,
  chargement,
  onOpenChange,
  onConfirmer,
}: AnnulerDemandeDialogProps) {
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

  useEffect(() => {
    if (open) {
      reset({ motif: "" });
    }
  }, [open, reset]);

  return (
    <AlertDialog open={open} onOpenChange={(next) => !chargement && onOpenChange(next)}>
      <AlertDialogContent className="rounded-3xl border-brand-900/10 bg-card">
        <AlertDialogHeader>
          <AlertDialogTitle className="text-ink-900">Annuler la demande</AlertDialogTitle>
          <AlertDialogDescription>
            Indiquez le motif de l&apos;annulation. Cette action est définitive tant que le
            laboratoire n&apos;a pas accepté la demande.
          </AlertDialogDescription>
        </AlertDialogHeader>

        <form
          className="space-y-4"
          noValidate
          onSubmit={handleSubmit((valeurs) => onConfirmer(valeurs.motif))}
        >
          <div className="space-y-2">
            <Label htmlFor="motif-annulation">Motif d&apos;annulation</Label>
            <Textarea
              id="motif-annulation"
              rows={4}
              maxLength={2000}
              placeholder="Exemple : analyses finalement non nécessaires"
              disabled={chargement}
              value={motifField.field.value}
              onChange={motifField.field.onChange}
              onBlur={motifField.field.onBlur}
              aria-invalid={errors.motif ? true : undefined}
              aria-describedby={errors.motif ? "motif-annulation-erreur" : undefined}
            />
            {errors.motif ? (
              <p id="motif-annulation-erreur" role="alert" className="text-sm text-red-600">
                {errors.motif.message}
              </p>
            ) : null}
          </div>

          <AlertDialogFooter className="gap-2">
            <AlertDialogCancel disabled={chargement}>Conserver la demande</AlertDialogCancel>
            <Button type="submit" loading={chargement} disabled={chargement}>
              Confirmer l&apos;annulation
            </Button>
          </AlertDialogFooter>
        </form>
      </AlertDialogContent>
    </AlertDialog>
  );
}
