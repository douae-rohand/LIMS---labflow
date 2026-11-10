import { zodResolver } from "@hookform/resolvers/zod";
import { useController, useForm } from "react-hook-form";
import { z } from "zod";
import { useEffect } from "react";
import { Button, Input, Label, Textarea } from "@/components/lab";
import {
  classeConformite,
  libelleConformite,
} from "@/components/technicien/libelles";
import type { LigneEssaiDetailTechnicienDto, ResultatSaisiePayload } from "@/api/technicien/types";
import { useSaisirResultat } from "@/hooks/useTechnicienEssais";
import { cn } from "@/lib/utils";

/** Construit le schéma zod : commentaire obligatoire si un résultat existe déjà. */
function construireSchema(dejaSaisi: boolean) {
  return z
    .object({
      valeur: z
        .string()
        .trim()
        .min(1, "La valeur du résultat est obligatoire.")
        .max(255, "La valeur ne peut pas dépasser 255 caractères."),
      commentaire: z.string().trim().max(2000, "Le commentaire ne peut pas dépasser 2000 caractères."),
    })
    .superRefine((data, ctx) => {
      if (dejaSaisi && data.commentaire.length < 5) {
        ctx.addIssue({
          path: ["commentaire"],
          code: "custom",
          message: "Un commentaire (motif) est obligatoire pour modifier un résultat déjà saisi.",
        });
      }
    });
}

type Valeurs = z.infer<ReturnType<typeof construireSchema>>;

/** Affiche les limites de référence (lecture seule) à côté du champ valeur. */
function LimitesReference({ detail }: { detail: LigneEssaiDetailTechnicienDto }) {
  const { limiteMin, limiteMax, seuilCritiqueMin, seuilCritiqueMax } = detail;
  const aLimites = limiteMin !== null || limiteMax !== null;
  if (!aLimites) {
    return <p className="text-xs text-muted-foreground">Aucune limite de référence définie.</p>;
  }
  return (
    <div className="space-y-1 text-xs text-muted-foreground">
      <p>
        Plage de référence :{" "}
        <span className="font-semibold text-ink-900">
          {limiteMin ?? "—"} à {limiteMax ?? "—"}
          {detail.essai.unite ? ` ${detail.essai.unite}` : ""}
        </span>
      </p>
      {seuilCritiqueMin !== null && seuilCritiqueMax !== null && (
        <p>
          Seuils critiques :{" "}
          <span className="font-semibold text-red-700">
            {seuilCritiqueMin} à {seuilCritiqueMax}
          </span>
        </p>
      )}
    </div>
  );
}

/** Bloc saisie du résultat : valeur + unité + limites + commentaire (motif). */
export function FormResultat({ detail }: { detail: LigneEssaiDetailTechnicienDto }) {
  const dejaSaisi = detail.valeur !== null && detail.valeur.trim() !== "";
  const schema = construireSchema(dejaSaisi);

  const {
    control,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<Valeurs>({
    resolver: zodResolver(schema),
    defaultValues: { valeur: detail.valeur ?? "", commentaire: "" },
  });

  const valeurField = useController({ name: "valeur", control });
  const commentaireField = useController({ name: "commentaire", control });

  // Re-sync quand le détail change (ex. après une autre mutation)
  useEffect(() => {
    reset({ valeur: detail.valeur ?? "", commentaire: "" });
  }, [detail.ligneEssaiId, detail.valeur, detail.dateSaisie, reset]);

  const mutation = useSaisirResultat();

  const onSubmit = (valeurs: Valeurs) => {
    const payload: ResultatSaisiePayload =
      valeurs.commentaire.trim() === ""
        ? { valeur: valeurs.valeur.trim() }
        : { valeur: valeurs.valeur.trim(), commentaire: valeurs.commentaire.trim() };
    mutation.mutate({ id: detail.ligneEssaiId, payload });
  };

  const enCours = mutation.isPending;

  return (
    <form className="space-y-4" noValidate onSubmit={handleSubmit(onSubmit)}>
      <div className="space-y-2">
        <Label htmlFor="valeur-resultat">
          Résultat mesuré{detail.essai.unite ? ` (en ${detail.essai.unite})` : ""}
        </Label>
        <Input
          id="valeur-resultat"
          type="text"
          inputMode="decimal"
          maxLength={255}
          placeholder="Ex. 0,92"
          disabled={enCours}
          value={valeurField.field.value}
          onChange={valeurField.field.onChange}
          onBlur={valeurField.field.onBlur}
          aria-invalid={errors.valeur ? true : undefined}
          aria-describedby={errors.valeur ? "valeur-erreur" : "limites-reference"}
          className="text-base"
        />
        <div id="limites-reference">
          <LimitesReference detail={detail} />
        </div>
        {errors.valeur ? (
          <p id="valeur-erreur" role="alert" className="text-sm text-red-600">
            {errors.valeur.message}
          </p>
        ) : null}
      </div>

      {/* Conformité actuelle (si déjà saisie) */}
      {dejaSaisi && detail.niveauConformite !== "NON_EVALUE" && (
        <div aria-live="polite">
          <p className="text-xs font-medium text-muted-foreground">Conformité actuelle</p>
          <span
            className={cn(
              "mt-1 inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold",
              classeConformite(detail.niveauConformite),
            )}
          >
            {libelleConformite(detail.niveauConformite)}
          </span>
        </div>
      )}

      <div className="space-y-2">
        <Label htmlFor="commentaire-resultat">
          {dejaSaisi ? "Motif de modification (obligatoire)" : "Commentaire (facultatif)"}
        </Label>
        <Textarea
          id="commentaire-resultat"
          rows={3}
          maxLength={2000}
          placeholder={
            dejaSaisi
              ? "Expliquez pourquoi vous modifiez ce résultat déjà saisi."
              : "Précisez un contexte si utile (facultatif)."
          }
          disabled={enCours}
          value={commentaireField.field.value}
          onChange={commentaireField.field.onChange}
          onBlur={commentaireField.field.onBlur}
          aria-invalid={errors.commentaire ? true : undefined}
          aria-describedby={errors.commentaire ? "commentaire-erreur" : undefined}
        />
        {errors.commentaire ? (
          <p id="commentaire-erreur" role="alert" className="text-sm text-red-600">
            {errors.commentaire.message}
          </p>
        ) : null}
      </div>

      <Button type="submit" loading={enCours} disabled={enCours}>
        {dejaSaisi ? "Modifier le résultat" : "Enregistrer le résultat"}
      </Button>
    </form>
  );
}
