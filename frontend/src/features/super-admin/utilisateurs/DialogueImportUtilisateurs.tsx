import { useMutation } from "@tanstack/react-query";
import { useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/lab";
import {
  analyserImportUtilisateurs,
  confirmerImportUtilisateurs,
  telechargerModeleUtilisateurs,
  type RapportImportUtilisateur,
} from "@/api/plateformeUtilisateurs";
import { Dialogue } from "../Fenetres";
import { messageErreur } from "../texte";

export function DialogueImportUtilisateurs({
  ouvert,
  onFermer,
  onTermine,
}: {
  ouvert: boolean;
  onFermer: () => void;
  onTermine: () => void;
}) {
  const [etape, setEtape] = useState<1 | 2 | 3>(1);
  const [rapport, setRapport] = useState<RapportImportUtilisateur | null>(null);

  function fermer() {
    setEtape(1);
    setRapport(null);
    onFermer();
  }

  const modele = useMutation({
    mutationFn: telechargerModeleUtilisateurs,
    onError: (erreur) => toast.error(messageErreur(erreur, "Le modèle n'a pas pu être téléchargé.")),
  });
  const analyse = useMutation({
    mutationFn: analyserImportUtilisateurs,
    onSuccess: (resultat) => {
      setRapport(resultat);
      setEtape(2);
      toast.success("Analyse terminée. Aucun compte n'a été créé.");
    },
    onError: (erreur) => toast.error(messageErreur(erreur, "Le fichier n'a pas pu être analysé.")),
  });
  const confirmation = useMutation({
    mutationFn: () => confirmerImportUtilisateurs(rapport?.jetonAnalyse ?? ""),
    onSuccess: (resultat) => {
      setRapport(resultat);
      setEtape(3);
      onTermine();
      toast.success(resultat.message ?? "Importation terminée.");
    },
    onError: (erreur) => toast.error(messageErreur(erreur, "L'importation a échoué.")),
  });

  const erreurs = rapport?.lignes.filter((ligne) => !ligne.valide) ?? [];

  return (
    <Dialogue ouvert={ouvert} large titre="Importer des administrateurs" description="Trois étapes : fichier, analyse, confirmation. Chaque ligne doit avoir le rôle ADMINISTRATEUR. Seules les lignes encore valides sont créées." onFermer={fermer}>
      <p className="mb-3 text-xs font-semibold tracking-wide text-muted-foreground uppercase">Étape {etape} sur 3</p>
      {etape === 1 && (
        <div className="flex flex-col gap-3">
          <div className="flex flex-wrap gap-2">
            <Button variant="secondary" size="sm" onClick={() => modele.mutate("csv")}>Modèle CSV</Button>
            <Button variant="secondary" size="sm" onClick={() => modele.mutate("xlsx")}>Modèle Excel</Button>
          </div>
          <label className="text-sm font-semibold text-ink-900" htmlFor="fichier-utilisateurs">Fichier CSV ou Excel, 2 Mo et 500 lignes maximum</label>
          <input
            id="fichier-utilisateurs"
            type="file"
            accept=".csv,.xlsx"
            className="text-sm"
            onChange={(event) => {
              const fichier = event.target.files?.[0];
              if (fichier) analyse.mutate(fichier);
            }}
          />
        </div>
      )}
      {etape === 2 && rapport && (
        <div className="flex flex-col gap-3">
          <p className="text-sm text-ink-900">{rapport.valides} ligne{rapport.valides > 1 ? "s" : ""} valide{rapport.valides > 1 ? "s" : ""}, {rapport.invalides} en erreur. Rien n'est encore écrit.</p>
          {erreurs.length === 0 ? (
            <p className="text-sm text-muted-foreground">Aucune ligne en erreur.</p>
          ) : (
            <ul className="max-h-64 overflow-y-auto rounded-2xl bg-surface-alt p-3 text-sm">
              {erreurs.map((ligne) => (
                <li key={ligne.numeroLigne} className="border-b border-brand-900/10 py-2 last:border-0">
                  <span className="font-semibold text-ink-900">Ligne {ligne.numeroLigne}</span>
                  {ligne.email ? ` · ${ligne.email}` : ""}
                  <span className="mt-1 block text-muted-foreground">{ligne.erreurs.join(" ")}</span>
                </li>
              ))}
            </ul>
          )}
          <div className="flex justify-end gap-2">
            <Button variant="ghost" onClick={() => setEtape(1)}>Retour</Button>
            <Button disabled={rapport.valides === 0 || !rapport.jetonAnalyse || confirmation.isPending} onClick={() => confirmation.mutate()}>
              Créer les lignes valides
            </Button>
          </div>
        </div>
      )}
      {etape === 3 && rapport && (
        <div className="flex flex-col gap-3">
          <p className="text-sm text-ink-900">{rapport.enregistres} compte{rapport.enregistres > 1 ? "s" : ""} créé{rapport.enregistres > 1 ? "s" : ""}.</p>
          <p className="text-sm text-muted-foreground">Les invitations partent ensuite. Si un e-mail n'arrive pas, ouvrez la ligne du compte et choisissez « Renvoyer l'invitation ».</p>
          <div className="flex justify-end">
            <Button onClick={fermer}>Fermer</Button>
          </div>
        </div>
      )}
    </Dialogue>
  );
}
