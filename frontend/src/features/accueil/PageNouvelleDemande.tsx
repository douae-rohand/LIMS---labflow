import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { useNavigate } from "@tanstack/react-router";
import { Check, ClipboardPlus, UserPlus } from "lucide-react";
import { toast } from "sonner";
import {
  creerDemandeAccueil,
  listerCatalogueAccueil,
  listerClients,
  listerPatients,
} from "@/api/accueil";
import { Button, Input, Label, Textarea } from "@/components/lab";
import { Container } from "@/components/layout/Container";
import {
  BarreFiltres,
  contient,
  EnTeteResponsable,
  EtatChargement,
  FiltreSelect,
  messageErreur,
  Recherche,
} from "@/features/responsable/commun";
import { DialogueClient } from "./DialogueClient";
import { DialoguePatient } from "./DialoguePatient";
import { formaterMontant } from "./commun";

/**
 * Demande d'analyse enregistrée au guichet pour un client présent sur site.
 * Elle suit ensuite le même circuit qu'une demande en ligne : décision du responsable, etc.
 */
export function PageNouvelleDemande() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const clients = useQuery({ queryKey: ["accueil-clients"], queryFn: listerClients });
  const patients = useQuery({ queryKey: ["accueil-patients"], queryFn: listerPatients });
  const catalogue = useQuery({ queryKey: ["accueil-catalogue"], queryFn: listerCatalogueAccueil });

  const [clientId, setClientId] = useState("");
  const [patientId, setPatientId] = useState("");
  const [titre, setTitre] = useState("");
  const [objectif, setObjectif] = useState("");
  const [choisis, setChoisis] = useState<number[]>([]);
  const [recherche, setRecherche] = useState("");
  const [domaine, setDomaine] = useState("");
  const [clientOuvert, setClientOuvert] = useState(false);
  const [patientOuvert, setPatientOuvert] = useState(false);

  const essais = catalogue.data ?? [];
  const domaines = useMemo(() => {
    const parCode = new Map<string, string>();
    for (const e of essais) if (e.domaineCode) parCode.set(e.domaineCode, e.domaineLibelle ?? e.domaineCode);
    return [...parCode.entries()].sort((x, y) => x[1].localeCompare(y[1], "fr"));
  }, [essais]);
  const visibles = essais
    .filter((e) => !domaine || e.domaineCode === domaine)
    .filter((e) => !recherche.trim() || contient(e.designation, recherche) || contient(e.code, recherche));
  const selection = essais.filter((e) => choisis.includes(e.id));
  const total = selection.reduce((somme, e) => somme + e.prix, 0);
  const basculer = (id: number) =>
    setChoisis((courant) => (courant.includes(id) ? courant.filter((x) => x !== id) : [...courant, id]));

  const manquant = !clientId ? "Choisissez le client."
    : !titre.trim() ? "Indiquez l'intitulé de la demande."
      : choisis.length === 0 ? "Choisissez au moins une analyse." : null;

  const creation = useMutation({
    mutationFn: () => creerDemandeAccueil({
      clientUtilisateurId: Number(clientId),
      patientId: patientId ? Number(patientId) : undefined,
      titre: titre.trim(),
      objectif: objectif.trim() || undefined,
      essaiIds: choisis,
    }),
    onSuccess: async (demande) => {
      toast.success(`Demande ${demande.numero} enregistrée : elle attend la décision du responsable.`);
      await queryClient.invalidateQueries({ queryKey: ["accueil-demandes"] });
      void navigate({ to: "/app/suivi-demandes" });
    },
    onError: (erreur) => toast.error(messageErreur(erreur, "La demande n'a pas pu être enregistrée.")),
  });

  return (
    <Container className="py-8">
      <EnTeteResponsable
        icone={ClipboardPlus}
        titre="Nouvelle demande"
        description="Enregistrez la demande d'un client présent à l'accueil. Elle suit le même circuit qu'une demande en ligne : le responsable l'accepte ou la refuse, puis le client la suit dans son espace."
      />

      <div className="grid gap-5 xl:grid-cols-[minmax(0,1fr)_22rem]">
        <div className="flex min-w-0 flex-col gap-5">
          {/* Demandeur */}
          <section className="rounded-2xl border border-brand-900/10 bg-card p-5">
            <h2 className="mb-4 text-sm font-extrabold text-ink-900">1. Demandeur</h2>
            <div className="grid gap-4 md:grid-cols-2">
              <div className="flex flex-col gap-1.5">
                <div className="flex items-center justify-between">
                  <Label htmlFor="nd-client">Client *</Label>
                  <button type="button" onClick={() => setClientOuvert(true)}
                    className="inline-flex items-center gap-1 text-xs font-semibold text-brand-900 hover:underline">
                    <UserPlus className="size-3.5" aria-hidden="true" /> Nouveau client
                  </button>
                </div>
                <FiltreSelect className="w-full" libelle="Client" tous={clients.isLoading ? "Chargement…" : "Choisir le client"}
                  valeur={clientId} onChange={setClientId}
                  options={(clients.data ?? []).map((c) => [String(c.utilisateurId),
                    `${c.raisonSociale || c.nomComplet} — ${c.email}`])} />
              </div>
              <div className="flex flex-col gap-1.5">
                <div className="flex items-center justify-between">
                  <Label htmlFor="nd-patient">Patient (analyses médicales)</Label>
                  <button type="button" onClick={() => setPatientOuvert(true)}
                    className="inline-flex items-center gap-1 text-xs font-semibold text-brand-900 hover:underline">
                    <UserPlus className="size-3.5" aria-hidden="true" /> Nouveau patient
                  </button>
                </div>
                <FiltreSelect className="w-full" libelle="Patient" tous="Aucun patient"
                  valeur={patientId} onChange={setPatientId}
                  options={(patients.data ?? []).map((p) => [String(p.id), `${p.prenom} ${p.nom} (${p.code})`])} />
              </div>
              <div className="flex flex-col gap-1.5 md:col-span-2">
                <Label htmlFor="nd-titre">Intitulé *</Label>
                <Input id="nd-titre" maxLength={255} value={titre} onChange={(e) => setTitre(e.target.value)}
                  placeholder="Ex. Bilan sanguin annuel, analyse de l'eau du puits…" />
              </div>
              <div className="flex flex-col gap-1.5 md:col-span-2">
                <Label htmlFor="nd-objectif">Objectif (facultatif)</Label>
                <Textarea id="nd-objectif" rows={2} maxLength={2000} value={objectif} onChange={(e) => setObjectif(e.target.value)} />
              </div>
            </div>
          </section>

          {/* Analyses */}
          <section className="rounded-2xl border border-brand-900/10 bg-card p-5">
            <h2 className="mb-4 text-sm font-extrabold text-ink-900">2. Analyses demandées</h2>
            <BarreFiltres>
              <Recherche className="w-full lg:flex-1" valeur={recherche} onChange={setRecherche} placeholder="Rechercher une analyse…" />
              <FiltreSelect className="w-full lg:w-52" libelle="Filtrer par domaine" tous="Tous les domaines"
                valeur={domaine} onChange={setDomaine} options={domaines} />
            </BarreFiltres>
            <EtatChargement
              chargement={catalogue.isLoading}
              erreur={catalogue.isError}
              vide={!catalogue.isLoading && visibles.length === 0}
              messageVide={essais.length === 0 ? "Aucune analyse active dans le catalogue." : "Aucune analyse ne correspond."}
              onReessayer={() => void catalogue.refetch()}
            />
            {visibles.length > 0 && (
              <ul className="grid max-h-[28rem] gap-2 overflow-y-auto pr-1 md:grid-cols-2">
                {visibles.map((essai) => {
                  const coche = choisis.includes(essai.id);
                  return (
                    <li key={essai.id}>
                      <button
                        type="button"
                        aria-pressed={coche}
                        onClick={() => basculer(essai.id)}
                        className={`flex w-full items-start gap-3 rounded-xl border px-3 py-2.5 text-left transition-colors ${
                          coche ? "border-brand-900 bg-brand-900/5" : "border-brand-900/10 hover:border-brand-900/40"
                        }`}
                      >
                        <span className={`mt-0.5 flex size-5 shrink-0 items-center justify-center rounded-md border ${
                          coche ? "border-brand-900 bg-brand-900 text-primary-foreground" : "border-brand-900/30"
                        }`}>
                          {coche && <Check className="size-3.5" aria-hidden="true" />}
                        </span>
                        <span className="min-w-0 flex-1">
                          <span className="block text-sm font-semibold text-ink-900">{essai.designation}</span>
                          <span className="block text-xs text-muted-foreground">{essai.code} · {essai.domaineLibelle ?? "Sans domaine"}</span>
                        </span>
                        <span className="shrink-0 text-sm font-semibold text-ink-900">{formaterMontant(essai.prix)}</span>
                      </button>
                    </li>
                  );
                })}
              </ul>
            )}
          </section>
        </div>

        {/* Récapitulatif */}
        <aside className="h-fit rounded-2xl border border-brand-900/10 bg-card p-5 xl:sticky xl:top-4">
          <h2 className="text-sm font-extrabold text-ink-900">Récapitulatif</h2>
          {selection.length === 0 ? (
            <p className="mt-3 text-sm text-muted-foreground">Aucune analyse choisie.</p>
          ) : (
            <ul className="mt-3 flex flex-col gap-1.5 text-sm">
              {selection.map((e) => (
                <li key={e.id} className="flex items-start justify-between gap-2">
                  <span className="min-w-0 text-ink-900">{e.designation}</span>
                  <span className="shrink-0 text-muted-foreground">{formaterMontant(e.prix)}</span>
                </li>
              ))}
            </ul>
          )}
          <div className="mt-4 flex items-center justify-between border-t border-brand-900/10 pt-3">
            <span className="text-sm font-semibold text-ink-900">Total TTC</span>
            <span className="text-lg font-extrabold text-ink-900">{formaterMontant(total)}</span>
          </div>
          <p className="mt-2 text-xs text-muted-foreground">La facture sera émise si le responsable accepte la demande.</p>
          {manquant && <p className="mt-3 text-xs font-semibold text-amber-800">{manquant}</p>}
          <Button className="mt-4 w-full" loading={creation.isPending} disabled={manquant !== null} onClick={() => creation.mutate()}>
            Enregistrer la demande
          </Button>
        </aside>
      </div>

      <DialogueClient ouvert={clientOuvert}
        onFermer={() => { setClientOuvert(false); void queryClient.invalidateQueries({ queryKey: ["accueil-clients"] }); }} />
      <DialoguePatient ouvert={patientOuvert} patient={null}
        onFermer={() => { setPatientOuvert(false); void queryClient.invalidateQueries({ queryKey: ["accueil-patients"] }); }} />
    </Container>
  );
}
