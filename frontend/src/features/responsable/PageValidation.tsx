import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { ClipboardCheck } from "lucide-react";
import { toast } from "sonner";
import { Badge, Button, Label, Textarea } from "@/components/lab";
import { Container } from "@/components/layout/Container";
import {
  deciderResultat,
  historiqueValidations,
  listerResultatsAValider,
  type AnalyseTravail,
  type DecisionResultat,
} from "@/api/responsable";
import { Panneau } from "@/features/super-admin/Fenetres";
import {
  Actualisation,
  contient,
  EnTeteResponsable,
  EtatChargement,
  formaterDate,
  messageErreur,
  Recherche,
  reference,
} from "./commun";

const LIBELLES_DECISION: Record<DecisionResultat, string> = {
  VALIDER: "Valider",
  RETOURNER: "Retourner au technicien",
  REJETER: "Rejeter (à refaire)",
};

export function PageValidation() {
  const queryClient = useQueryClient();
  const [choisie, setChoisie] = useState<AnalyseTravail | null>(null);
  const [recherche, setRecherche] = useState("");
  const resultats = useQuery({
    queryKey: ["responsable-a-valider"],
    queryFn: listerResultatsAValider,
  });
  // Le serveur trie déjà : la dernière validation technique en haut.
  const visibles = (resultats.data ?? []).filter((a) => !recherche.trim()
    || contient(a.demandeReference, recherche)
    || contient(a.essaiLibelle, recherche)
    || contient(a.technicienNom, recherche));

  const validationRapide = useMutation({
    mutationFn: (analyse: AnalyseTravail) => deciderResultat(analyse.id, "VALIDER"),
    onSuccess: async (_donnees, analyse) => {
      toast.success(`${analyse.essaiLibelle ?? "Résultat"} validé.`);
      await actualiser();
    },
    onError: (erreur) => toast.error(messageErreur(erreur, "La validation a échoué.")),
  });

  const actualiser = async () => {
    await queryClient.invalidateQueries({ queryKey: ["responsable-a-valider"] });
    await queryClient.invalidateQueries({ queryKey: ["responsable-suivi"] });
    await queryClient.invalidateQueries({ queryKey: ["responsable-rapports"] });
  };

  return (
    <Container className="py-8">
      <EnTeteResponsable
        icone={ClipboardCheck}
        titre="Validation des résultats"
        description="Résultats validés techniquement par le technicien, en attente de votre décision. Les plus récents sont en haut."
        action={
          <Actualisation
            misAJour={resultats.dataUpdatedAt}
            enCours={resultats.isFetching}
            onActualiser={() => void resultats.refetch()}
          />
        }
      />

      {(resultats.data?.length ?? 0) > 0 && (
        <div className="mb-4 flex justify-end">
          <Recherche valeur={recherche} onChange={setRecherche} placeholder="Rechercher une demande, une analyse…" />
        </div>
      )}

      <EtatChargement
        chargement={resultats.isLoading}
        erreur={resultats.isError}
        vide={!resultats.isLoading && visibles.length === 0}
        messageVide={recherche.trim() ? "Aucun résultat ne correspond à la recherche." : "Aucun résultat en attente de validation."}
        onReessayer={() => void resultats.refetch()}
      />

      {visibles.length > 0 && (
        <div className="overflow-x-auto rounded-2xl border border-brand-900/10 bg-card">
          <table className="w-full min-w-[820px] text-left text-sm">
            <thead>
              <tr className="bg-surface-alt text-xs font-semibold tracking-wide text-brand-900 uppercase">
                <th className="px-4 py-3">Analyse</th>
                <th className="px-4 py-3">Demande</th>
                <th className="px-4 py-3">Résultat</th>
                <th className="px-4 py-3">Référence</th>
                <th className="px-4 py-3">Technicien</th>
                <th className="px-4 py-3">Soumis le</th>
                <th className="px-4 py-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody>
              {visibles.map((analyse) => (
                <tr key={analyse.id} className="border-t border-brand-900/10">
                  <td className="px-4 py-3 font-semibold text-ink-900">{analyse.essaiLibelle ?? analyse.code}</td>
                  <td className="px-4 py-3 text-muted-foreground">{analyse.demandeReference}</td>
                  <td className="px-4 py-3">
                    <span className="font-semibold text-ink-900">{analyse.valeur} {analyse.unite ?? ""}</span>{" "}
                    {analyse.conformite != null && (
                      <Badge size="sm" variant={analyse.conformite ? "mint" : "dark"}>{analyse.conformite ? "Conforme" : "Hors norme"}</Badge>
                    )}
                  </td>
                  <td className="px-4 py-3 text-muted-foreground">{reference(analyse)}</td>
                  <td className="px-4 py-3">{analyse.technicienNom ?? "—"}</td>
                  <td className="px-4 py-3 text-muted-foreground">{formaterDate(analyse.derniereDecisionDate, true)}</td>
                  <td className="px-4 py-3 text-right">
                    <div className="flex justify-end gap-2">
                      <Button size="sm" variant="secondary" onClick={() => setChoisie(analyse)}>Examiner</Button>
                      <Button
                        size="sm"
                        loading={validationRapide.isPending && validationRapide.variables?.id === analyse.id}
                        disabled={validationRapide.isPending}
                        onClick={() => validationRapide.mutate(analyse)}
                      >
                        Valider
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <PanneauDecision
        analyse={choisie}
        onFermer={() => setChoisie(null)}
        onDecide={async () => {
          setChoisie(null);
          await actualiser();
        }}
      />
    </Container>
  );
}

function PanneauDecision({ analyse, onFermer, onDecide }: {
  analyse: AnalyseTravail | null;
  onFermer: () => void;
  onDecide: () => Promise<void>;
}) {
  const [decision, setDecision] = useState<DecisionResultat>("VALIDER");
  const [motif, setMotif] = useState("");

  useEffect(() => {
    setDecision("VALIDER");
    setMotif("");
  }, [analyse?.id]);

  const historique = useQuery({
    queryKey: ["responsable-historique-validation", analyse?.id],
    queryFn: () => historiqueValidations(analyse?.id as number),
    enabled: analyse != null,
  });

  const envoi = useMutation({
    mutationFn: () => deciderResultat(analyse?.id as number, decision, motif),
    onSuccess: async () => {
      toast.success(decision === "VALIDER" ? "Résultat validé." : decision === "RETOURNER" ? "Résultat retourné au technicien." : "Résultat rejeté : l'essai est à refaire.");
      await onDecide();
    },
    onError: (erreur) => toast.error(messageErreur(erreur, "La décision n'a pas pu être enregistrée.")),
  });

  const motifRequis = decision !== "VALIDER";

  return (
    <Panneau ouvert={analyse != null} titre={analyse?.essaiLibelle ?? "Résultat"} onFermer={onFermer}>
      {analyse && (
        <div className="flex flex-col gap-5 text-sm">
          <dl className="grid gap-3 sm:grid-cols-2">
            <Info libelle="Demande" valeur={`${analyse.demandeReference}${analyse.demandeTitre ? ` · ${analyse.demandeTitre}` : ""}`} />
            <Info libelle="Méthode" valeur={analyse.methode ?? "—"} />
            <Info libelle="Résultat" valeur={`${analyse.valeur ?? "—"} ${analyse.unite ?? ""}`} />
            <Info libelle="Valeurs de référence" valeur={reference(analyse)} />
            <Info libelle="Conformité" valeur={analyse.conformite == null ? "Non calculée" : analyse.conformite ? "Conforme" : "Hors norme"} />
            <Info libelle="Technicien" valeur={analyse.technicienNom ?? "—"} />
            <Info libelle="Saisi le" valeur={formaterDate(analyse.dateSaisie, true)} />
            <Info libelle="Observations" valeur={analyse.observations ?? "—"} />
          </dl>

          <section>
            <h3 className="font-bold text-ink-900">Historique de validation</h3>
            <ol className="mt-2 flex flex-col gap-2">
              {historique.data?.map((v) => (
                <li key={v.id} className="rounded-2xl bg-surface-alt px-3 py-2">
                  <p className="font-semibold text-ink-900">
                    Niveau {v.niveau} · {v.statut === "VALIDEE" ? "Validé" : v.statut === "REJETEE" ? "Rejeté" : v.statut === "REVISION_DEMANDEE" ? "Retourné" : "—"}
                  </p>
                  <p className="text-xs text-muted-foreground">{v.validateurNom ?? "—"} · {formaterDate(v.dateValidation, true)}</p>
                  {v.commentaire && <p className="mt-1">{v.commentaire}</p>}
                </li>
              ))}
            </ol>
          </section>

          <fieldset className="flex flex-col gap-2">
            <legend className="mb-1 font-bold text-ink-900">Votre décision</legend>
            {(Object.keys(LIBELLES_DECISION) as DecisionResultat[]).map((valeur) => (
              <label key={valeur} className={`flex items-center gap-3 rounded-2xl border px-4 py-3 font-semibold ${decision === valeur ? "border-brand-900 bg-brand-900/5" : "border-brand-900/10"}`}>
                <input type="radio" name="decision" className="size-4 accent-brand-900" checked={decision === valeur} onChange={() => setDecision(valeur)} />
                {LIBELLES_DECISION[valeur]}
              </label>
            ))}
          </fieldset>
          <div>
            <Label htmlFor="motif-decision">{motifRequis ? "Motif (communiqué au technicien) *" : "Commentaire (facultatif)"}</Label>
            <Textarea id="motif-decision" className="mt-1" maxLength={2000} value={motif} onChange={(event) => setMotif(event.target.value)} />
          </div>
          <div className="flex justify-end gap-2 border-t border-brand-900/10 pt-4">
            <Button variant="ghost" onClick={onFermer}>Annuler</Button>
            <Button loading={envoi.isPending} disabled={motifRequis && !motif.trim()} onClick={() => envoi.mutate()}>
              Confirmer
            </Button>
          </div>
        </div>
      )}
    </Panneau>
  );
}

function Info({ libelle, valeur }: { libelle: string; valeur: string }) {
  return (
    <div className="rounded-2xl bg-surface-alt px-3 py-2">
      <dt className="text-xs font-semibold text-muted-foreground">{libelle}</dt>
      <dd className="text-ink-900">{valeur}</dd>
    </div>
  );
}
