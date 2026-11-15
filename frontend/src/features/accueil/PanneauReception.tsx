import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useCallback, useEffect, useState } from "react";
import { Plus, Printer, Tag, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { Badge, Button, Input, Label, Textarea } from "@/components/lab";
import {
  lireReception,
  ouvrirEtiquettes,
  receptionner,
  type DetailReception,
  type EchantillonARecevoir,
  type LigneReception,
} from "@/api/accueil";
import { Panneau } from "@/features/super-admin/Fenetres";
import { EtatChargement, formaterDate, messageErreur } from "@/features/responsable/commun";
import { BadgeEtatReception, demandeur } from "./commun";
import { DialogueEtiquette } from "./DialogueEtiquette";

/** Échantillon en cours de saisie dans le formulaire de réception. */
interface Brouillon {
  cle: number;
  nature: string;
  conforme: boolean;
  motif: string;
  etat: string;
  indication?: string | undefined;
  ligneIds: number[];
}

let compteur = 0;

/** Un échantillon proposé par nature attendue, avec les analyses qu'il couvre. */
function brouillonsInitiaux(detail: DetailReception): Brouillon[] {
  const dejaPrises = new Set<number>();
  return detail.naturesAttendues.map((nature) => {
    const ligneIds = nature.ligneIds.filter((id) => !dejaPrises.has(id));
    ligneIds.forEach((id) => dejaPrises.add(id));
    compteur += 1;
    return {
      cle: compteur,
      nature: nature.nature ?? "",
      conforme: true,
      motif: "",
      etat: "",
      indication: [nature.etatConservation, nature.quantite].filter(Boolean).join(" · ") || undefined,
      ligneIds,
    };
  }).filter((b) => b.ligneIds.length > 0);
}

function libelleLigne(ligne: LigneReception): string {
  return `${ligne.essai ?? ligne.essaiCode ?? ligne.code} · ${ligne.code}`;
}

export function PanneauReception({ demandeId, onFermer }: { demandeId: number | undefined; onFermer: () => void }) {
  const queryClient = useQueryClient();
  const detail = useQuery({
    queryKey: ["accueil-reception", demandeId],
    queryFn: () => lireReception(demandeId as number),
    enabled: demandeId != null,
  });
  const [brouillons, setBrouillons] = useState<Brouillon[]>([]);
  /** Échantillon dont l'étiquette est affichée. */
  const [etiquette, setEtiquette] = useState<number | null>(null);
  const fermerEtiquette = useCallback(() => setEtiquette(null), []);

  // Une proposition par nature attendue, à l'ouverture d'une demande et après chaque réception :
  // une actualisation en direct ne doit pas effacer la saisie en cours.
  const donnees = detail.data;
  const [initialisePour, setInitialisePour] = useState<number | null>(null);
  useEffect(() => {
    if (donnees && initialisePour !== donnees.demande.demandeId) {
      setBrouillons(brouillonsInitiaux(donnees));
      setInitialisePour(donnees.demande.demandeId);
    }
  }, [donnees, initialisePour]);
  useEffect(() => {
    if (demandeId == null) setInitialisePour(null);
  }, [demandeId]);

  const envoi = useMutation({
    mutationFn: (echantillons: EchantillonARecevoir[]) => receptionner(demandeId as number, echantillons),
    onSuccess: async (resultat) => {
      toast.success("Réception enregistrée. Le client et les techniciens ont été prévenus.");
      queryClient.setQueryData(["accueil-reception", demandeId], resultat);
      setBrouillons(brouillonsInitiaux(resultat));
      await queryClient.invalidateQueries({ queryKey: ["accueil-receptions"] });
      await queryClient.invalidateQueries({ queryKey: ["accueil-tableau"] });
    },
    onError: (erreur) => toast.error(messageErreur(erreur, "La réception n'a pas pu être enregistrée.")),
  });

  const aRecevoir = (donnees?.lignes ?? []).filter((l) => l.aRecevoir);
  const modifier = (cle: number, changement: Partial<Brouillon>) =>
    setBrouillons((liste) => liste.map((b) => (b.cle === cle ? { ...b, ...changement } : b)));
  const basculerLigne = (cle: number, ligneId: number, cochee: boolean) =>
    setBrouillons((liste) => liste.map((b) => {
      if (b.cle === cle) {
        return { ...b, ligneIds: cochee ? [...b.ligneIds, ligneId] : b.ligneIds.filter((id) => id !== ligneId) };
      }
      // Une analyse n'appartient qu'à un seul échantillon.
      return cochee ? { ...b, ligneIds: b.ligneIds.filter((id) => id !== ligneId) } : b;
    }));
  const ajouter = () => {
    compteur += 1;
    setBrouillons((liste) => [...liste, { cle: compteur, nature: "", conforme: true, motif: "", etat: "", ligneIds: [] }]);
  };

  const erreurs = brouillons.flatMap((b, index) => {
    const nom = b.nature.trim() || `n° ${index + 1}`;
    return [
      !b.nature.trim() ? `Échantillon ${nom} : indiquez la nature.` : null,
      b.ligneIds.length === 0 ? `Échantillon ${nom} : cochez au moins une analyse.` : null,
      !b.conforme && !b.motif.trim() ? `Échantillon ${nom} : le motif de non-conformité est obligatoire.` : null,
    ].filter((m): m is string => m != null);
  });

  const enregistrer = () => {
    if (brouillons.length === 0 || erreurs.length > 0) return;
    envoi.mutate(brouillons.map((b) => ({
      nature: b.nature.trim(),
      conforme: b.conforme,
      motif: b.conforme ? undefined : b.motif.trim(),
      etatConservationConstate: b.etat.trim() || undefined,
      ligneIds: b.ligneIds,
    })));
  };

  return (
    <Panneau ouvert={demandeId != null} titre={`Réception · ${donnees?.demande.reference ?? ""}`} onFermer={onFermer} large>
      <EtatChargement chargement={detail.isLoading} erreur={detail.isError} vide={false} messageVide=""
        onReessayer={() => void detail.refetch()} />

      {donnees && (
        <div className="flex flex-col gap-6 text-sm">
          <div className="flex flex-wrap items-center gap-2">
            <BadgeEtatReception demande={donnees.demande} />
            <span className="text-muted-foreground">
              {demandeur(donnees.demande)}{donnees.demande.titre ? ` · ${donnees.demande.titre}` : ""}
              {" "}· {donnees.demande.recues}/{donnees.demande.analyses} analyse(s) couvertes
            </span>
          </div>

          <section>
            <h3 className="mb-2 font-bold text-ink-900">Analyses de la demande</h3>
            <div className="overflow-x-auto rounded-2xl border border-brand-900/10">
              <table className="w-full min-w-[640px] text-left">
                <thead>
                  <tr className="bg-surface-alt text-xs font-semibold tracking-wide text-brand-900 uppercase">
                    <th className="px-3 py-2">Analyse</th>
                    <th className="px-3 py-2">Échantillon requis</th>
                    <th className="px-3 py-2">Technicien</th>
                    <th className="px-3 py-2">Échantillon</th>
                  </tr>
                </thead>
                <tbody>
                  {donnees.lignes.map((ligne) => (
                    <tr key={ligne.id} className="border-t border-brand-900/10 align-top">
                      <td className="px-3 py-2">
                        <p className="font-semibold text-ink-900">{ligne.essai ?? ligne.essaiCode}</p>
                        <p className="text-xs text-muted-foreground">{ligne.code}{ligne.domaine ? ` · ${ligne.domaine}` : ""}</p>
                      </td>
                      <td className="px-3 py-2 text-xs text-muted-foreground">
                        {ligne.exigences.length === 0 ? "Non précisé" : ligne.exigences.map((x) => (
                          <p key={x.nature}>
                            {x.nature}{x.etatConservation ? ` · ${x.etatConservation}` : ""}
                            {x.quantiteMinimale != null ? ` · min. ${x.quantiteMinimale}${x.unite ? ` ${x.unite}` : ""}` : ""}
                          </p>
                        ))}
                      </td>
                      <td className="px-3 py-2 text-xs">{ligne.technicien ?? "Non affectée"}</td>
                      <td className="px-3 py-2">
                        {ligne.statut === "NON_REALISABLE" ? (
                          <Badge size="sm" variant="outline">Non réalisable</Badge>
                        ) : ligne.echantillonReference && ligne.echantillonConforme ? (
                          <Badge size="sm" variant="mint">{ligne.echantillonReference}</Badge>
                        ) : ligne.echantillonReference ? (
                          <Badge size="sm" variant="dark">{ligne.echantillonReference} · non conforme</Badge>
                        ) : (
                          <Badge size="sm" variant="lime">À recevoir</Badge>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>

          {aRecevoir.length > 0 && (
            <section className="rounded-2xl border border-brand-900/15 bg-surface-alt/40 p-4">
              <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
                <div>
                  <h3 className="font-bold text-ink-900">Nouvelle réception</h3>
                  <p className="text-xs text-muted-foreground">
                    Un échantillon est proposé par nature attendue. Retirez ceux qui n'ont pas été déposés : la réception peut être partielle.
                  </p>
                </div>
                <Button size="sm" variant="secondary" icon={<Plus className="size-4" aria-hidden="true" />} iconPosition="left" onClick={ajouter}>
                  Ajouter un échantillon
                </Button>
              </div>

              <div className="flex flex-col gap-3">
                {brouillons.map((brouillon, index) => (
                  <fieldset key={brouillon.cle} className="rounded-2xl border border-brand-900/10 bg-card p-4">
                    <legend className="sr-only">Échantillon {index + 1}</legend>
                    <div className="grid gap-3 md:grid-cols-2">
                      <div className="flex flex-col gap-1.5">
                        <Label htmlFor={`nature-${brouillon.cle}`}>Nature</Label>
                        <Input id={`nature-${brouillon.cle}`} value={brouillon.nature} maxLength={100}
                          placeholder="ex. Sang total, Eau de surface"
                          onChange={(event) => modifier(brouillon.cle, { nature: event.target.value })} />
                        {brouillon.indication && <p className="text-xs text-muted-foreground">Attendu : {brouillon.indication}</p>}
                      </div>
                      <div className="flex flex-col gap-1.5">
                        <Label htmlFor={`etat-${brouillon.cle}`}>État constaté (facultatif)</Label>
                        <Input id={`etat-${brouillon.cle}`} value={brouillon.etat} maxLength={500}
                          placeholder="ex. Tube réfrigéré, 5 mL"
                          onChange={(event) => modifier(brouillon.cle, { etat: event.target.value })} />
                      </div>
                    </div>

                    <div className="mt-3 flex flex-wrap gap-2" role="radiogroup" aria-label="Conformité">
                      {[true, false].map((valeur) => (
                        <label key={String(valeur)} className={`inline-flex cursor-pointer items-center gap-2 rounded-xl border px-3 py-1.5 text-sm font-semibold ${
                          brouillon.conforme === valeur
                            ? valeur ? "border-brand-900 bg-brand-900/5 text-brand-900" : "border-red-300 bg-red-50 text-red-700"
                            : "border-brand-900/10 text-muted-foreground"
                        }`}>
                          <input type="radio" className="size-4 accent-brand-900" name={`conformite-${brouillon.cle}`}
                            checked={brouillon.conforme === valeur} onChange={() => modifier(brouillon.cle, { conforme: valeur })} />
                          {valeur ? "Conforme" : "Non conforme"}
                        </label>
                      ))}
                    </div>
                    {!brouillon.conforme && (
                      <div className="mt-3 flex flex-col gap-1.5">
                        <Label htmlFor={`motif-${brouillon.cle}`}>Motif de non-conformité</Label>
                        <Textarea id={`motif-${brouillon.cle}`} className="min-h-20" value={brouillon.motif} maxLength={1000}
                          placeholder="ex. Tube hémolysé, quantité insuffisante, chaîne du froid rompue"
                          onChange={(event) => modifier(brouillon.cle, { motif: event.target.value })} />
                        <p className="text-xs text-muted-foreground">Les analyses concernées resteront en attente d'un nouvel échantillon.</p>
                      </div>
                    )}

                    <div className="mt-3">
                      <p className="mb-1.5 text-xs font-semibold text-ink-900">Analyses couvertes</p>
                      <div className="flex flex-wrap gap-2">
                        {aRecevoir.map((ligne) => {
                          const cochee = brouillon.ligneIds.includes(ligne.id);
                          return (
                            <label key={ligne.id} className={`inline-flex cursor-pointer items-center gap-2 rounded-xl border px-3 py-1.5 text-xs ${
                              cochee ? "border-brand-900 bg-brand-900/5 text-brand-900" : "border-brand-900/10 text-muted-foreground"
                            }`}>
                              <input type="checkbox" className="size-4 accent-brand-900" checked={cochee}
                                onChange={(event) => basculerLigne(brouillon.cle, ligne.id, event.target.checked)} />
                              {libelleLigne(ligne)}
                            </label>
                          );
                        })}
                      </div>
                    </div>

                    <div className="mt-3 flex justify-end">
                      <Button size="sm" variant="ghost" icon={<Trash2 className="size-4" aria-hidden="true" />} iconPosition="left"
                        onClick={() => setBrouillons((liste) => liste.filter((b) => b.cle !== brouillon.cle))}>
                        Non déposé
                      </Button>
                    </div>
                  </fieldset>
                ))}
                {brouillons.length === 0 && (
                  <p className="text-xs text-muted-foreground">Aucun échantillon à enregistrer. Ajoutez-en un pour commencer.</p>
                )}
              </div>

              {erreurs.length > 0 && brouillons.length > 0 && (
                <ul className="mt-3 list-disc pl-5 text-xs text-red-700">
                  {erreurs.map((erreur) => <li key={erreur}>{erreur}</li>)}
                </ul>
              )}
              <div className="mt-4 flex justify-end">
                <Button loading={envoi.isPending} disabled={brouillons.length === 0 || erreurs.length > 0} onClick={enregistrer}>
                  Enregistrer la réception ({brouillons.length})
                </Button>
              </div>
            </section>
          )}

          <section>
            <div className="mb-2 flex flex-wrap items-center justify-between gap-2">
              <h3 className="font-bold text-ink-900">Échantillons reçus</h3>
              {donnees.echantillons.length > 0 && (
                <Button size="sm" variant="secondary" icon={<Printer className="size-4" aria-hidden="true" />} iconPosition="left"
                  onClick={() => void ouvrirEtiquettes(donnees.demande.demandeId)
                    .catch((erreur: unknown) => toast.error(messageErreur(erreur, "Les étiquettes n'ont pas pu être générées.")))}>
                  Imprimer les étiquettes
                </Button>
              )}
            </div>
            {donnees.echantillons.length === 0 ? (
              <EtatChargement chargement={false} erreur={false} vide messageVide="Aucun échantillon reçu pour cette demande." />
            ) : (
              <ul className="flex flex-col gap-2">
                {donnees.echantillons.map((echantillon) => (
                  <li key={echantillon.id}>
                    <button type="button" onClick={() => setEtiquette(echantillon.id)}
                      title="Voir l'étiquette"
                      className="group w-full rounded-2xl border border-brand-900/10 bg-card px-4 py-3 text-left transition hover:border-brand-900/30 hover:bg-surface-alt focus-visible:outline-2 focus-visible:outline-brand-900">
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <p className="font-semibold text-ink-900">{echantillon.reference} · {echantillon.nature}</p>
                      <div className="flex items-center gap-2">
                        <span className="inline-flex items-center gap-1 text-xs font-semibold text-brand-900 opacity-70 group-hover:opacity-100">
                          <Tag className="size-3.5" aria-hidden="true" /> Étiquette
                        </span>
                        <Badge size="sm" variant={echantillon.conforme ? "mint" : "dark"}>
                          {echantillon.conforme ? "Conforme" : "Non conforme"}
                        </Badge>
                      </div>
                    </div>
                    <p className="text-xs text-muted-foreground">
                      Reçu le {formaterDate(echantillon.dateReception, true)}
                      {echantillon.receptionneur ? ` par ${echantillon.receptionneur}` : ""}
                      {echantillon.analyses.length > 0 ? ` · ${echantillon.analyses.join(", ")}` : ""}
                    </p>
                    {echantillon.etatConservationConstate && (
                      <p className="text-xs text-muted-foreground">État constaté : {echantillon.etatConservationConstate}</p>
                    )}
                    {!echantillon.conforme && echantillon.motif && (
                      <p className="mt-1 text-xs font-semibold text-red-700">Motif : {echantillon.motif}</p>
                    )}
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </section>
          <DialogueEtiquette
            echantillon={donnees.echantillons.find((e) => e.id === etiquette) ?? null}
            demandeId={donnees.demande.demandeId}
            demandeReference={donnees.demande.reference}
            demandeur={donnees.demande.patient ?? donnees.demande.client ?? "Client"}
            laboratoire={donnees.laboratoire}
            onFermer={fermerEtiquette}
          />
        </div>
      )}
    </Panneau>
  );
}
