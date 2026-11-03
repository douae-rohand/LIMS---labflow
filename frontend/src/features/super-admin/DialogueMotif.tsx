import { useState } from "react";
import { Button, Label, Textarea } from "@/components/lab";
import { Dialogue } from "./Fenetres";

export function DialogueMotif({
  ouvert,
  titre,
  description,
  libelleConfirmation,
  motifObligatoire,
  enCours,
  onConfirmer,
  onFermer,
}: {
  ouvert: boolean;
  titre: string;
  description: string;
  libelleConfirmation: string;
  motifObligatoire?: boolean;
  enCours?: boolean;
  onConfirmer: (motif: string) => void;
  onFermer: () => void;
}) {
  const [motif, setMotif] = useState("");
  const valide = !motifObligatoire || motif.trim().length >= 10;

  return (
    <Dialogue
      ouvert={ouvert}
      titre={titre}
      description={description}
      onFermer={() => {
        setMotif("");
        onFermer();
      }}
    >
      <div className="flex flex-col gap-3">
        <div>
          <Label htmlFor="motif-action">{motifObligatoire ? "Motif" : "Motif (facultatif)"}</Label>
          <Textarea
            id="motif-action"
            value={motif}
            onChange={(event) => setMotif(event.target.value)}
            rows={3}
            maxLength={2000}
          />
        </div>
        <div className="flex justify-end gap-2">
          <Button variant="ghost" onClick={onFermer}>Annuler</Button>
          <Button disabled={!valide || enCours} onClick={() => onConfirmer(motif.trim())}>
            {libelleConfirmation}
          </Button>
        </div>
      </div>
    </Dialogue>
  );
}
