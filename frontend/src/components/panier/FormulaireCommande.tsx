import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useController, useForm } from "react-hook-form";
import { useState } from "react";
import { toast } from "sonner";
import { z } from "zod";
import { Upload, X } from "lucide-react";
import {
  creerDemandeDepuisPanier,
  messageErreurDemandeClient,
  type DemandeDetailClient,
  type PieceJointeUpload,
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
  const [piecesJointes, setPiecesJointes] = useState<PieceJointeUpload[]>([]);

  const handleAjouterPieceJointe = (event: React.ChangeEvent<HTMLInputElement>) => {
    const fichier = event.target.files?.[0];
    if (!fichier) return;
    if (piecesJointes.length >= 2) {
      toast.error("Maximum 2 pièces jointes autorisées.");
      return;
    }
    const reader = new FileReader();
    reader.onload = (e) => {
      const contenu = e.target?.result as ArrayBuffer;
      setPiecesJointes((courant) => [
        ...courant,
        {
          nomFichier: fichier.name,
          typeMime: fichier.type,
          contenu,
        },
      ]);
    };
    reader.readAsArrayBuffer(fichier);
    event.target.value = "";
  };

  const handleSupprimerPieceJointe = (index: number) => {
    setPiecesJointes((courant) => courant.filter((_, i) => i !== index));
  };

  const mutation = useMutation({
    mutationFn: (values: CommandeFormValues) =>
      creerDemandeDepuisPanier(laboratoireId, {
        titre: values.titre,
        ...(values.objectif.trim().length > 0 ? { objectif: values.objectif } : {}),
        ...(piecesJointes.length > 0 ? { piecesJointes } : {}),
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

      <div className="flex flex-col gap-3">
        <Label htmlFor="commande-pieces-jointes">Pièces jointes (max 2)</Label>
        <div className="flex items-center gap-3">
          <Label htmlFor="commande-pieces-jointes" className="shrink-0">
            <Upload className="size-4" aria-hidden="true" />
          </Label>
          <Input
            id="commande-pieces-jointes"
            type="file"
            accept=".pdf,.doc,.docx,.jpg,.jpeg,.png"
            disabled={mutation.isPending || piecesJointes.length >= 2}
            onChange={handleAjouterPieceJointe}
            className="flex-1"
          />
          <span className="text-xs text-muted-foreground">{piecesJointes.length}/2</span>
        </div>
        {piecesJointes.length > 0 && (
          <ul className="flex flex-col gap-2">
            {piecesJointes.map((piece, index) => (
              <li key={index} className="flex items-center justify-between rounded-lg border border-brand-900/10 bg-surface-alt px-3 py-2">
                <span className="text-sm text-ink-900">{piece.nomFichier}</span>
                <button
                  type="button"
                  onClick={() => handleSupprimerPieceJointe(index)}
                  disabled={mutation.isPending}
                  className="shrink-0 text-muted-foreground hover:text-ink-900 disabled:opacity-50"
                >
                  <X className="size-4" aria-hidden="true" />
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>

      <Button type="submit" disabled={mutation.isPending} loading={mutation.isPending} className="mt-2">
        {mutation.isPending ? "Envoi…" : "Finaliser la demande"}
      </Button>
    </form>
  );
}
