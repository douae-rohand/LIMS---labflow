import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { toast } from "sonner";
import { Badge, Button, Label, Textarea } from "@/components/lab";
import { IndicateurEtapes } from "@/components/integration/IndicateurEtapes";
import { LocationPicker } from "@/components/integration/LocationPicker";
import {
  ajouterDocumentIntegration,
  demanderComplements,
  LIBELLES_STATUT_INTEGRATION,
  renvoyerInvitation,
  trouverDemandeIntegration,
  traiterDemandeIntegration,
} from "@/api/plateforme";
import { ListeDocuments } from "../documents/ListeDocuments";
import { Dialogue, Panneau } from "../Fenetres";
import { formaterDate, messageErreur, texte } from "../texte";

const ETAPES = ["Laboratoire", "Localisation", "Administrateur", "Documents"] as const;

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
  const [complementsOuvert, setComplementsOuvert] = useState(false);
  const [messageComplements, setMessageComplements] = useState("");
  const [documentsReclames, setDocumentsReclames] = useState<string[]>([]);

  useEffect(() => {
    setEtape(0);
    setMotif("");
    setConfirmation(null);
    setComplementsOuvert(false);
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
    mutationFn: (decision: "ACCEPTEE" | "REFUSEE") =>
      traiterDemandeIntegration(demandeId as number, decision, decision === "REFUSEE" ? motif : undefined),
    onSuccess: async (_data, decision) => {
      toast.success(
        decision === "ACCEPTEE"
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
    mutationFn: ({ type, fichier }: { type: string; fichier: File }) =>
      ajouterDocumentIntegration(demandeId as number, type, fichier),
    onSuccess: async () => {
      toast.success("Le document a été enregistré.");
      await invalider();
    },
    onError: (erreur) => toast.error(messageErreur(erreur, "Le document n'a pas pu être enregistré.")),
  });

  const complements = useMutation({
    mutationFn: () => demanderComplements(demandeId as number, messageComplements.trim(), documentsReclames),
    onSuccess: async () => {
      toast.success("La demande de compléments a été envoyée au demandeur.");
      setComplementsOuvert(false);
      setMessageComplements("");
      setDocumentsReclames([]);
      await invalider();
    },
    onError: (erreur) => toast.error(messageErreur(erreur, "La demande de compléments a échoué.")),
  });

  const invitation = useMutation({
    mutationFn: () => renvoyerInvitation(demandeId as number),
    onSuccess: () => toast.success("Le lien d'activation a été renvoyé."),
    onError: (erreur) => toast.error(messageErreur(erreur, "L'envoi du lien a échoué.")),
  });

  const dossier = demande.data;
  const enAttente = dossier?.statut === "EN_ATTENTE" || dossier?.statut === "COMPLEMENTS_DEMANDES";
  const attenteDemandeur = dossier?.statut === "COMPLEMENTS_DEMANDES";
  const documentsActifs = (dossier?.documentsAttendus ?? []).filter((type) => type.actif);

  const ouvrirComplements = () => {
    const presents = new Set((dossier?.documents ?? []).map((document) => document.typeDocument));
    setDocumentsReclames(
      documentsActifs.filter((type) => type.obligatoire && !presents.has(type.code)).map((type) => type.code),
    );
    setMessageComplements("");
    setComplementsOuvert(true);
  };
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
              <Badge variant={attenteDemandeur ? "lime" : "soft"}>
                {LIBELLES_STATUT_INTEGRATION[dossier.statut]}
              </Badge>
              <span className="text-muted-foreground">
                {dossier.numero} · {formaterDate(dossier.dateSoumission)}
              </span>
            </div>

            {attenteDemandeur && (
              <div className="rounded-2xl border border-brand-900/15 bg-surface-alt px-4 py-3">
                <p className="font-semibold text-ink-900">En attente du demandeur</p>
                <p className="mt-1 text-xs text-muted-foreground">
                  Demandé le {formaterDate(dossier.dateDemandeComplements)}
                </p>
                {dossier.complementsDemandes && <p className="mt-2 whitespace-pre-line">{dossier.complementsDemandes}</p>}
              </div>
            )}

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
                {dossier.informationsComplementaires && (
                  <p className="whitespace-pre-line">{dossier.informationsComplementaires}</p>
                )}
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
                {dossier.statut === "ACCEPTEE" && (
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
                  attendus={dossier.documentsAttendus}
                  demandes={attenteDemandeur ? dossier.documentsComplements : undefined}
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
                        Le dossier est incomplet. L'acceptation reste impossible tant que tous les documents obligatoires ne sont pas présents.
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
                      <Button variant="secondary" onClick={ouvrirComplements}>
                        {attenteDemandeur ? "Relancer le demandeur" : "Demander des compléments"}
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
        ouvert={complementsOuvert}
        titre="Demander des compléments"
        description="Le demandeur reçoit un e-mail avec un lien à usage unique, valable 14 jours, pour répondre et déposer les pièces."
        onFermer={() => setComplementsOuvert(false)}
      >
        <div className="flex flex-col gap-4 text-sm">
          <div>
            <Label htmlFor="message-complements">Message au demandeur *</Label>
            <Textarea
              id="message-complements"
              className="mt-1"
              rows={4}
              maxLength={4000}
              value={messageComplements}
              onChange={(event) => setMessageComplements(event.target.value)}
              placeholder="Précisez ce qui manque ou ce qui doit être corrigé."
            />
          </div>
          {documentsActifs.length > 0 && (
            <fieldset>
              <legend className="font-semibold text-ink-900">Documents à déposer</legend>
              <p className="text-xs text-muted-foreground">
                Les documents cochés devront être envoyés par le demandeur. Laissez vide pour une simple question.
              </p>
              <div className="mt-2 flex flex-col gap-2">
                {documentsActifs.map((type) => (
                  <label key={type.code} className="flex items-start gap-2 text-ink-900">
                    <input
                      type="checkbox"
                      className="mt-0.5 size-4 accent-brand-900"
                      checked={documentsReclames.includes(type.code)}
                      onChange={(event) =>
                        setDocumentsReclames((courant) =>
                          event.target.checked
                            ? [...courant, type.code]
                            : courant.filter((code) => code !== type.code),
                        )
                      }
                    />
                    <span>{type.libelle}</span>
                  </label>
                ))}
              </div>
            </fieldset>
          )}
          <div className="flex justify-end gap-2">
            <Button variant="ghost" onClick={() => setComplementsOuvert(false)}>
              Annuler
            </Button>
            <Button
              loading={complements.isPending}
              disabled={!messageComplements.trim()}
              onClick={() => complements.mutate()}
            >
              Envoyer la demande
            </Button>
          </div>
        </div>
      </Dialogue>

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
              traitement.mutate(confirmation === "accepter" ? "ACCEPTEE" : "REFUSEE");
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
