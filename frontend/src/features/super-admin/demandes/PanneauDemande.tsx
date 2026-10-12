import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { toast } from "sonner";
import { Badge, Button, Label, Textarea } from "@/components/lab";
import { IndicateurEtapes } from "@/components/integration/IndicateurEtapes";
import { LocationPicker } from "@/components/integration/LocationPicker";
import { TYPES_DOCUMENTS } from "@/components/integration/constants";
import {
  ajouterDocumentIntegration,
  renvoyerInvitation,
  trouverDemandeIntegration,
  traiterDemandeIntegration,
  type StatutIntegration,
} from "@/api/plateforme";
import { ListeDocuments } from "../documents/ListeDocuments";
import { Dialogue, Panneau } from "../Fenetres";
import { formaterDate, messageErreur, texte } from "../texte";

const ETAPES = ["Laboratoire", "Localisation", "Administrateur", "Documents"] as const;

const LIBELLES: Record<StatutIntegration, string> = {
  EN_ATTENTE: "En attente",
  EN_COURS_VALIDATION: "En cours de validation",
  APPROUVEE: "Acceptée",
  REJETEE: "Refusée",
  SUSPENDUE: "Suspendue",
};

export function PanneauDemande({
  demandeId,
  onFermer,
}: {
  demandeId: number | null;
  onFermer: () => void;
}) {
  const queryClient = useQueryClient();
  const [etape, setEtape] = useState(0);
  const [motif, setMotif] = useState("");
  const [confirmation, setConfirmation] = useState<"accepter" | "refuser" | null>(null);

  useEffect(() => {
    setEtape(0);
    setMotif("");
    setConfirmation(null);
  }, [demandeId]);

  const demande = useQuery({
    queryKey: ["demande-integration", demandeId],
    queryFn: () => trouverDemandeIntegration(demandeId as number),
    enabled: demandeId != null,
  });

  const invalider = async () => {
    await queryClient.invalidateQueries({ queryKey: ["demande-integration", demandeId] });
    await queryClient.invalidateQueries({ queryKey: ["demandes-integration"] });
    await queryClient.invalidateQueries({ queryKey: ["laboratoires-admin"] });
    await queryClient.invalidateQueries({ queryKey: ["demande-laboratoire"] });
    await queryClient.invalidateQueries({ queryKey: ["document-apercu"] });
    await queryClient.invalidateQueries({ queryKey: ["statistiques-plateforme"] });
  };

  const traitement = useMutation({
    mutationFn: (decision: "APPROUVEE" | "REJETEE") =>
      traiterDemandeIntegration(demandeId as number, decision, decision === "REJETEE" ? motif : undefined),
    onSuccess: async (_data, decision) => {
      toast.success(
        decision === "APPROUVEE"
          ? "La demande est acceptée. L'espace du laboratoire et le compte administrateur ont été créés."
          : "La demande a été refusée.",
      );
      setConfirmation(null);
      setMotif("");
      await invalider();
    },
    onError: (erreur) => toast.error(messageErreur(erreur, "Le traitement a échoué.")),
  });

  const documentMutation = useMutation({
    mutationFn: ({ type, fichier }: { type: (typeof TYPES_DOCUMENTS)[number]; fichier: File }) =>
      ajouterDocumentIntegration(demandeId as number, type, fichier),
    onSuccess: async () => {
      toast.success("Le document a été enregistré.");
      await invalider();
    },
    onError: (erreur) => toast.error(messageErreur(erreur, "Le document n'a pas pu être enregistré.")),
  });

  const invitation = useMutation({
    mutationFn: () => renvoyerInvitation(demandeId as number),
    onSuccess: () => toast.success("Le lien d'activation a été renvoyé."),
    onError: (erreur) => toast.error(messageErreur(erreur, "L'envoi du lien a échoué.")),
  });

  const dossier = demande.data;
  const enAttente = dossier?.statut === "EN_ATTENTE" || dossier?.statut === "EN_COURS_VALIDATION";
  const aDesCoordonnees = dossier?.latitude != null && dossier?.longitude != null;

  return (
    <>
      <Panneau
        ouvert={demandeId != null}
        large
        titre={dossier?.nomLaboratoire ?? "Demande d'intégration"}
        onFermer={onFermer}
      >
        {demande.isLoading && <p className="text-sm text-muted-foreground">Chargement…</p>}
        {demande.isError && <p className="text-sm text-red-600">Cette demande est introuvable.</p>}
        {dossier && (
          <div className="flex flex-col gap-5 text-sm">
            <div className="flex flex-wrap items-center gap-2">
              <Badge>{LIBELLES[dossier.statut]}</Badge>
              <span className="text-muted-foreground">
                {dossier.numero} · {formaterDate(dossier.dateSoumission)}
              </span>
            </div>

            <IndicateurEtapes etapes={ETAPES} etape={etape} onAller={setEtape} autoriserSuivantes />

            {etape === 0 && (
              <dl className="grid gap-3 sm:grid-cols-2">
                <Lecture libelle="Nom du laboratoire" valeur={dossier.nomLaboratoire} />
                <Lecture libelle="Raison sociale" valeur={texte(dossier.raisonSociale)} />
                <Lecture libelle="Types" valeur={(dossier.typesLaboratoire ?? []).join(", ") || "Non renseigné"} />
                <Lecture libelle="Identifiant ICE" valeur={texte(dossier.ice)} />
                <Lecture libelle="Téléphone" valeur={texte(dossier.telephoneLaboratoire)} />
                <Lecture libelle="Adresse e-mail" valeur={texte(dossier.emailLaboratoire)} />
                <Lecture libelle="Site web" valeur={texte(dossier.siteWeb)} />
                {dossier.nomSchema && <Lecture libelle="Espace dédié" valeur={dossier.nomSchema} />}
              </dl>
            )}

            {etape === 1 && (
              <div className="flex flex-col gap-4">
                <dl className="grid gap-3 sm:grid-cols-2">
                  <Lecture libelle="Adresse" valeur={texte(dossier.adresse)} />
                  <Lecture libelle="Ville" valeur={texte(dossier.ville)} />
                  <Lecture libelle="Région" valeur={texte(dossier.region)} />
                  <Lecture libelle="Code postal" valeur={texte(dossier.codePostal)} />
                  <Lecture libelle="Pays" valeur={texte(dossier.pays)} />
                </dl>
                {aDesCoordonnees ? (
                  <LocationPicker
                    readOnly
                    label={dossier.nomLaboratoire}
                    value={{ latitude: dossier.latitude ?? null, longitude: dossier.longitude ?? null }}
                    onChange={() => undefined}
                  />
                ) : (
                  <p className="text-muted-foreground">Aucune coordonnée géographique n'est enregistrée.</p>
                )}
                {dossier.informationsComplementaires && <p>{dossier.informationsComplementaires}</p>}
              </div>
            )}

            {etape === 2 && (
              <div className="flex flex-col gap-4">
                <dl className="grid gap-3 sm:grid-cols-2">
                  <Lecture libelle="Prénom" valeur={texte(dossier.adminPrenom)} />
                  <Lecture libelle="Nom" valeur={texte(dossier.adminNom)} />
                  <Lecture libelle="Adresse e-mail" valeur={dossier.adminEmail} />
                  <Lecture libelle="Téléphone" valeur={texte(dossier.adminTelephone)} />
                  <Lecture libelle="Fonction" valeur={texte(dossier.adminFonction)} />
                  <Lecture libelle="CIN" valeur={texte(dossier.adminCin)} />
                </dl>
                {dossier.statut === "APPROUVEE" && (
                  <Button variant="secondary" loading={invitation.isPending} onClick={() => invitation.mutate()}>
                    Renvoyer le lien d'activation
                  </Button>
                )}
              </div>
            )}

            {etape === 3 && (
              <div className="flex flex-col gap-4">
                <ListeDocuments
                  demandeId={dossier.id}
                  documents={dossier.documents}
                  peutRemplacer={enAttente}
                  remplacementEnCours={documentMutation.isPending}
                  onRemplacer={(type, fichier) => documentMutation.mutate({ type, fichier })}
                />
                <section>
                  <h3 className="font-extrabold text-ink-900">Historique et statut</h3>
                  <ol className="mt-2 flex flex-col gap-2">
                    {(dossier.historique ?? []).map((evenement) => (
                      <li key={`${evenement.evenement}-${evenement.date}`} className="rounded-2xl bg-surface-alt px-3 py-2">
                        <p className="font-semibold text-ink-900">{evenement.evenement}</p>
                        <p className="text-xs text-muted-foreground">{formaterDate(evenement.date)}</p>
                        {evenement.detail && <p className="mt-1">{evenement.detail}</p>}
                      </li>
                    ))}
                  </ol>
                </section>
                {enAttente && (
                  <div className="flex flex-col gap-3">
                    {!dossier.dossierComplet && (
                      <p className="rounded-2xl bg-surface-alt px-3 py-2 text-ink-900">
                        Le dossier est incomplet. L'acceptation reste impossible tant que les six documents ne sont pas présents.
                      </p>
                    )}
                    <div>
                      <Label htmlFor="motif-refus">Motif du refus *</Label>
                      <Textarea
                        id="motif-refus"
                        className="mt-1"
                        value={motif}
                        onChange={(event) => setMotif(event.target.value)}
                      />
                    </div>
                    <div className="flex flex-wrap gap-2">
                      <Button disabled={!dossier.dossierComplet} onClick={() => setConfirmation("accepter")}>
                        Accepter et ouvrir l'espace
                      </Button>
                      <Button variant="ghost" onClick={() => setConfirmation("refuser")}>
                        Refuser la demande
                      </Button>
                    </div>
                  </div>
                )}
              </div>
            )}

            <div className="flex flex-wrap items-center justify-between gap-2 border-t border-brand-900/10 pt-4">
              <Button type="button" variant="ghost" onClick={onFermer}>
                Fermer
              </Button>
              <div className="flex gap-2">
                <Button type="button" variant="secondary" disabled={etape === 0} onClick={() => setEtape((actuel) => Math.max(0, actuel - 1))}>
                  Précédent
                </Button>
                {etape < ETAPES.length - 1 && (
                  <Button type="button" onClick={() => setEtape((actuel) => actuel + 1)}>
                    Suivant
                  </Button>
                )}
              </div>
            </div>
          </div>
        )}
      </Panneau>

      <Dialogue
        ouvert={confirmation !== null}
        titre={confirmation === "accepter" ? "Accepter cette demande ?" : "Refuser cette demande ?"}
        description={
          confirmation === "accepter"
            ? "L'espace du laboratoire, son schéma et le compte administrateur seront créés. Cette décision est définitive."
            : "La demande sera refusée et le motif sera communiqué au demandeur."
        }
        onFermer={() => setConfirmation(null)}
      >
        <div className="flex justify-end gap-2">
          <Button variant="ghost" onClick={() => setConfirmation(null)}>
            Annuler
          </Button>
          <Button
            loading={traitement.isPending}
            onClick={() => {
              if (confirmation === "refuser" && !motif.trim()) {
                toast.error("Le motif du refus est obligatoire.");
                return;
              }
              traitement.mutate(confirmation === "accepter" ? "APPROUVEE" : "REJETEE");
            }}
          >
            Confirmer
          </Button>
        </div>
      </Dialogue>
    </>
  );
}

function Lecture({ libelle, valeur }: { libelle: string; valeur: string }) {
  return (
    <div className="rounded-2xl bg-surface-alt px-3 py-2">
      <dt className="text-xs font-semibold text-muted-foreground">{libelle}</dt>
      <dd className="text-sm text-ink-900">{valeur}</dd>
    </div>
  );
}
