import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Button, Label, Textarea } from "@/components/lab";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";

const schema = z.object({
  motif: z
    .string()
    .trim()
    .min(5, "Le motif doit contenir au moins 5 caractères.")
    .max(2000, "Le motif ne peut pas dépasser 2000 caractères."),
});

type Valeurs = z.infer<typeof schema>;

interface RefusDemandeModalProps {
  open: boolean;
  chargement: boolean;
  erreur: string | null;
  onOpenChange: (open: boolean) => void;
  onConfirmer: (motif: string) => void;
}

export function RefusDemandeModal({
  open,
  chargement,
  erreur,
  onOpenChange,
  onConfirmer,
}: RefusDemandeModalProps) {
  const form = useForm<Valeurs>({
    resolver: zodResolver(schema),
    defaultValues: { motif: "" },
  });

  useEffect(() => {
    if (open) {
      form.reset({ motif: "" });
    }
  }, [open, form]);

  return (
    <Dialog open={open} onOpenChange={(next) => !chargement && onOpenChange(next)}>
      <DialogContent className="rounded-3xl border-brand-900/10">
        <DialogHeader>
          <DialogTitle className="text-ink-900">Refuser la demande</DialogTitle>
          <DialogDescription>
            Indiquez pourquoi cette demande ne peut pas être acceptée. Le motif est obligatoire.
          </DialogDescription>
        </DialogHeader>
        <form
          className="space-y-4"
          onSubmit={form.handleSubmit((valeurs) => onConfirmer(valeurs.motif))}
        >
          <div className="space-y-2">
            <Label htmlFor="motif-refus">Motif du refus</Label>
            <Textarea
              id="motif-refus"
              rows={4}
              maxLength={2000}
              placeholder="Exemple : analyses hors du catalogue du laboratoire"
              disabled={chargement}
              aria-invalid={form.formState.errors.motif ? true : undefined}
              aria-describedby={form.formState.errors.motif ? "motif-refus-erreur" : undefined}
              {...form.register("motif")}
            />
            {form.formState.errors.motif && (
              <p id="motif-refus-erreur" className="text-sm text-red-600">
                {form.formState.errors.motif.message}
              </p>
            )}
            {erreur && <p className="text-sm text-red-600">{erreur}</p>}
          </div>
          <DialogFooter className="gap-2">
            <Button
              type="button"
              variant="secondary"
              onClick={() => onOpenChange(false)}
              disabled={chargement}
            >
              Annuler
            </Button>
            <Button type="submit" loading={chargement} disabled={chargement}>
              Confirmer le refus
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
