import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useController, useForm } from "react-hook-form";
import { toast } from "sonner";
import { z } from "zod";
import {
  creerDemandeDepuisPanier,
  messageErreurDemandeClient,
  type DemandeDetailClient,
} from "@/api/demandesClient";
import { Button, Input, Label, Textarea } from "@/components/lab";
import { PANIERS_QUERY_KEY } from "@/hooks/usePanier";

const schemaCommande = z.object({
  titre: z
    .string()
    .trim()
    .min(1, "Le titre de la demande est obligatoire.")
    .max(255, "255 caractères maximum."),
  objectif: z.string().trim().max(2000, "2000 caractères maximum."),
});

type CommandeFormValues = z.infer<typeof schemaCommande>;

/**
 * Formulaire de validation de commande (RHF + zod). Les champs utilisent
 * `useController` car les composants de `components/lab` n'exposent pas de ref.
 */
export function FormulaireCommande({
  laboratoireId,
  onCree,
}: {
  laboratoireId: number;
  onCree: (demande: DemandeDetailClient) => void;
}) {
  const queryClient = useQueryClient();
  const {
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<CommandeFormValues>({
    resolver: zodResolver(schemaCommande),
    defaultValues: { titre: "", objectif: "" },
  });

  const titreField = useController({ name: "titre", control });
  const objectifField = useController({ name: "objectif", control });

  const mutation = useMutation({
    mutationFn: (values: CommandeFormValues) =>
      creerDemandeDepuisPanier(laboratoireId, {
        titre: values.titre,
        ...(values.objectif.trim().length > 0 ? { objectif: values.objectif } : {}),
      }),
    onSuccess: (demande) => {
      void queryClient.invalidateQueries({ queryKey: PANIERS_QUERY_KEY });
      toast.success("Demande envoyée au laboratoire.");
      onCree(demande);
    },
    onError: (error) => {
      toast.error(messageErreurDemandeClient(error));
    },
  });

  return (
    <form
      onSubmit={handleSubmit((values) => mutation.mutate(values))}
      className="flex flex-col gap-3"
      noValidate
    >
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="commande-titre">Titre de la demande</Label>
        <Input
          id="commande-titre"
          value={titreField.field.value}
          onChange={titreField.field.onChange}
          onBlur={titreField.field.onBlur}
          disabled={mutation.isPending}
          placeholder="Ex : Analyses d'eau potable - lot 12"
          aria-invalid={!!errors.titre}
          aria-describedby={errors.titre ? "commande-titre-error" : undefined}
        />
        {errors.titre ? (
          <p id="commande-titre-error" role="alert" className="text-xs text-red-600">
            {errors.titre.message}
          </p>
        ) : null}
      </div>

      <div className="flex flex-col gap-1.5">
        <Label htmlFor="commande-objectif">Objectif (optionnel)</Label>
        <Textarea
          id="commande-objectif"
          value={objectifField.field.value}
          onChange={objectifField.field.onChange}
          onBlur={objectifField.field.onBlur}
          disabled={mutation.isPending}
          placeholder="Contexte et résultats attendus"
          aria-invalid={!!errors.objectif}
          aria-describedby={errors.objectif ? "commande-objectif-error" : undefined}
        />
        {errors.objectif ? (
          <p id="commande-objectif-error" role="alert" className="text-xs text-red-600">
            {errors.objectif.message}
          </p>
        ) : null}
      </div>

      <Button type="submit" disabled={mutation.isPending} loading={mutation.isPending} className="mt-2">
        {mutation.isPending ? "Envoi…" : "Finaliser la demande"}
      </Button>
    </form>
  );
}
