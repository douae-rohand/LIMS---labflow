import { zodResolver } from "@hookform/resolvers/zod";
import { useController, useForm } from "react-hook-form";
import { z } from "zod";
import { Button, Label, Textarea } from "@/components/lab";
import type { EnqueteDetailClient, RepondreEnqueteBody } from "@/api/enquetesClient";
import { NOTE_MAX, NOTE_MIN } from "./libelles";
import { NotesRadio } from "./NotesRadio";

const schema = z.object({
  noteGlobale: z
    .number()
    .int()
    .min(NOTE_MIN, "Sélectionnez une note globale.")
    .max(NOTE_MAX),
  noteDelai: z.number().int().min(NOTE_MIN).max(NOTE_MAX).nullable(),
  noteClarte: z.number().int().min(NOTE_MIN).max(NOTE_MAX).nullable(),
  noteRelation: z.number().int().min(NOTE_MIN).max(NOTE_MAX).nullable(),
  commentaire: z
    .string()
    .trim()
    .max(2000, "Le commentaire ne peut pas dépasser 2000 caractères."),
});

type Valeurs = z.infer<typeof schema>;

interface FormulaireEnqueteProps {
  enquete: EnqueteDetailClient;
  enregistrement: boolean;
  onRepondre: (body: RepondreEnqueteBody) => void;
}

/** Saisie de la réponse à une enquête : note globale obligatoire, reste facultatif. */
export function FormulaireEnquete({
  enquete,
  enregistrement,
  onRepondre,
}: FormulaireEnqueteProps) {
  const {
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<Valeurs>({
    resolver: zodResolver(schema),
    defaultValues: {
      noteGlobale: 0,
      noteDelai: enquete.noteDelai,
      noteClarte: enquete.noteClarte,
      noteRelation: enquete.noteRelation,
      commentaire: enquete.commentaire ?? "",
    },
  });

  const globale = useController({ name: "noteGlobale", control });
  const delai = useController({ name: "noteDelai", control });
  const clarte = useController({ name: "noteClarte", control });
  const relation = useController({ name: "noteRelation", control });
  const commentaire = useController({ name: "commentaire", control });

  function soumettre(valeurs: Valeurs) {
    const body: RepondreEnqueteBody = { noteGlobale: valeurs.noteGlobale };
    if (valeurs.noteDelai !== null) body.noteDelai = valeurs.noteDelai;
    if (valeurs.noteClarte !== null) body.noteClarte = valeurs.noteClarte;
    if (valeurs.noteRelation !== null) body.noteRelation = valeurs.noteRelation;
    const texte = valeurs.commentaire.trim();
    if (texte.length > 0) body.commentaire = texte;
    onRepondre(body);
  }

  return (
    <form
      className="flex flex-col gap-6"
      noValidate
      onSubmit={handleSubmit(soumettre)}
    >
      <NotesRadio
        id="enquete-note-globale"
        legende="Satisfaction globale"
        obligatoire
        valeur={globale.field.value}
        onChange={globale.field.onChange}
        disabled={enregistrement}
        erreur={errors.noteGlobale?.message}
      />

      <div className="grid gap-6 sm:grid-cols-3">
        <NotesRadio
          id="enquete-note-delai"
          legende="Respect des délais"
          valeur={delai.field.value}
          onChange={delai.field.onChange}
          disabled={enregistrement}
        />
        <NotesRadio
          id="enquete-note-clarte"
          legende="Clarté du rapport"
          valeur={clarte.field.value}
          onChange={clarte.field.onChange}
          disabled={enregistrement}
        />
        <NotesRadio
          id="enquete-note-relation"
          legende="Qualité de l'accueil"
          valeur={relation.field.value}
          onChange={relation.field.onChange}
          disabled={enregistrement}
        />
      </div>

      <div className="space-y-2">
        <Label htmlFor="enquete-commentaire">Commentaire (facultatif)</Label>
        <Textarea
          id="enquete-commentaire"
          rows={4}
          maxLength={2000}
          placeholder="Précisez votre retour…"
          disabled={enregistrement}
          value={commentaire.field.value}
          onChange={commentaire.field.onChange}
          onBlur={commentaire.field.onBlur}
          aria-invalid={errors.commentaire ? true : undefined}
          aria-describedby={errors.commentaire ? "enquete-commentaire-erreur" : undefined}
        />
        {errors.commentaire ? (
          <p id="enquete-commentaire-erreur" role="alert" className="text-sm text-red-600">
            {errors.commentaire.message}
          </p>
        ) : null}
      </div>

      <div>
        <Button type="submit" loading={enregistrement} disabled={enregistrement}>
          Envoyer ma réponse
        </Button>
      </div>
    </form>
  );
}
