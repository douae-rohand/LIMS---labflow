import { zodResolver } from "@hookform/resolvers/zod";
import { useController, useForm } from "react-hook-form";
import { z } from "zod";
import { useEffect } from "react";
import { AlertTriangle, CheckCircle2, FlaskConical, Info, PenLine, TriangleAlert } from "lucide-react";
import { Button, Input, Label, Textarea } from "@/components/lab";
import { classeConformite, libelleConformite } from "@/components/technicien/libelles";
import type { LigneEssaiDetailTechnicienDto, ResultatSaisiePayload } from "@/api/technicien/types";
import { useSaisirResultat } from "@/hooks/useTechnicienEssais";
import { cn } from "@/lib/utils";

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

/** Encart plage de référence + seuils critiques. */
function LimitesReference({ detail }: { detail: LigneEssaiDetailTechnicienDto }) {
  const { limiteMin, limiteMax, seuilCritiqueMin, seuilCritiqueMax } = detail;
  const aLimites = limiteMin !== null || limiteMax !== null;
  const aSeuilsCritiques = seuilCritiqueMin !== null && seuilCritiqueMax !== null;

  if (!aLimites) return null;

  return (
    <div className="flex flex-wrap gap-3 rounded-2xl border border-brand-900/10 bg-surface-alt px-4 py-3">
      <div className="flex items-start gap-2">
        <Info className="mt-0.5 size-3.5 shrink-0 text-brand-900" aria-hidden="true" />
        <div>
          <p className="text-xs font-semibold text-muted-foreground">Plage de référence</p>
          <p className="text-sm font-semibold text-ink-900">
            {limiteMin ?? "—"} — {limiteMax ?? "—"}
            {detail.essai.unite ? <span className="ml-1 font-normal text-muted-foreground">{detail.essai.unite}</span> : null}
          </p>
        </div>
      </div>
      {aSeuilsCritiques && (
        <div className="flex items-start gap-2">
          <TriangleAlert className="mt-0.5 size-3.5 shrink-0 text-red-500" aria-hidden="true" />
          <div>
            <p className="text-xs font-semibold text-muted-foreground">Seuils critiques</p>
            <p className="text-sm font-semibold text-red-700">
              {seuilCritiqueMin} — {seuilCritiqueMax}
            </p>
          </div>
        </div>
      )}
    </div>
  );
}

/** Badge conformité avec icône. */
function BadgeConformite({ niveau }: { niveau: string }) {
  const estOk = niveau === "CONFORME" || niveau === "NORMAL";
  const icone = estOk
    ? <CheckCircle2 className="size-3.5" aria-hidden="true" />
    : <AlertTriangle className="size-3.5" aria-hidden="true" />;
  return (
    <span className={cn("inline-flex items-center gap-1 rounded-full border px-2.5 py-0.5 text-xs font-semibold", classeConformite(niveau))}>
      {icone}
      {libelleConformite(niveau)}
    </span>
  );
}

export function FormResultat({ detail }: { detail: LigneEssaiDetailTechnicienDto }) {
  const dejaSaisi = detail.valeur !== null && detail.valeur.trim() !== "";
  const schema = construireSchema(dejaSaisi);

  const {
    control,
    handleSubmit,
    reset,
    formState: { errors, isDirty },
  } = useForm<Valeurs>({
    resolver: zodResolver(schema),
    defaultValues: { valeur: detail.valeur ?? "", commentaire: "" },
  });

  const valeurField = useController({ name: "valeur", control });
  const commentaireField = useController({ name: "commentaire", control });

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
    <form className="space-y-5" noValidate onSubmit={handleSubmit(onSubmit)}>

      {/* Bannière modification */}
      {dejaSaisi && (
        <div className="flex items-start gap-3 rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3">
          <PenLine className="mt-0.5 size-4 shrink-0 text-amber-700" aria-hidden="true" />
          <div>
            <p className="text-sm font-semibold text-amber-900">Résultat déjà saisi</p>
            <p className="text-xs text-amber-800 mt-0.5">
              Vous modifiez un résultat existant. Un motif de modification sera requis.
            </p>
          </div>
        </div>
      )}

      {/* Champ valeur */}
      <div className="space-y-2">
        <Label htmlFor="valeur-resultat" className="flex items-center gap-1.5">
          <FlaskConical className="size-3.5 text-brand-900" aria-hidden="true" />
          Résultat mesuré
          {detail.essai.unite && (
            <span className="text-xs font-normal text-muted-foreground">(en {detail.essai.unite})</span>
          )}
        </Label>
        <Input
          id="valeur-resultat"
          type="text"
          inputMode="decimal"
          maxLength={255}
          placeholder={detail.essai.unite ? `Ex. 5.4 ${detail.essai.unite}` : "Ex. 0,92"}
          disabled={enCours}
          value={valeurField.field.value}
          onChange={valeurField.field.onChange}
          onBlur={valeurField.field.onBlur}
          aria-invalid={errors.valeur ? true : undefined}
          aria-describedby={errors.valeur ? "valeur-erreur" : "limites-reference"}
          className="text-base"
        />
        {errors.valeur ? (
          <p id="valeur-erreur" role="alert" className="text-sm text-red-600">
            {errors.valeur.message}
          </p>
        ) : null}
      </div>

      {/* Plage de référence */}
      <div id="limites-reference">
        <LimitesReference detail={detail} />
      </div>

      {/* Conformité actuelle */}
      {dejaSaisi && detail.niveauConformite !== "NON_EVALUE" && (
        <div className="flex items-center gap-2" aria-live="polite">
          <span className="text-xs text-muted-foreground">Conformité actuelle :</span>
          <BadgeConformite niveau={detail.niveauConformite} />
        </div>
      )}

      {/* Commentaire */}
      <div className="space-y-2">
        <Label htmlFor="commentaire-resultat">
          {dejaSaisi ? (
            <span className="flex items-center gap-1">
              Motif de modification
              <span className="text-red-500 text-xs">*</span>
            </span>
          ) : (
            "Commentaire"
          )}
          {!dejaSaisi && <span className="ml-1 text-xs font-normal text-muted-foreground">(facultatif)</span>}
        </Label>
        <Textarea
          id="commentaire-resultat"
          rows={3}
          maxLength={2000}
          placeholder={
            dejaSaisi
              ? "Expliquez pourquoi vous modifiez ce résultat."
              : "Précisez un contexte si utile."
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

      {/* Bouton */}
      <Button
        type="submit"
        loading={enCours}
        disabled={enCours || !isDirty}
        icon={dejaSaisi
          ? <PenLine className="size-4" aria-hidden="true" />
          : <CheckCircle2 className="size-4" aria-hidden="true" />
        }
        iconPosition="left"
        className="w-full sm:w-auto"
      >
        {dejaSaisi ? "Modifier le résultat" : "Enregistrer le résultat"}
      </Button>
    </form>
  );
}
