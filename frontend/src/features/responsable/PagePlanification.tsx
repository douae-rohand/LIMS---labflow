import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link } from "@tanstack/react-router";
import { useEffect, useMemo, useState } from "react";
import { CalendarClock, UserCheck } from "lucide-react";
import { toast } from "sonner";
import { Badge, Button, Input, Label, Textarea } from "@/components/lab";
import { Container } from "@/components/layout/Container";
import {
  affecterAnalyses,
  listerAnalyses,
  listerTechniciens,
  type AnalyseTravail,
  type FiltreAnalyses,
} from "@/api/responsable";
import { Dialogue } from "@/features/super-admin/Fenetres";
import {
  Actualisation,
  BarreFiltres,
  BadgeStatutAnalyse,
  contient,
  EnTeteResponsable,
  EtatChargement,
  formaterDate,
  messageErreur,
  FiltreSelect,
  Recherche,
} from "./commun";

const ONGLETS: { id: FiltreAnalyses; libelle: string }[] = [
  { id: "A_AFFECTER", libelle: "À affecter" },
  { id: "AFFECTEES", libelle: "Affectées" },
  { id: "EN_RETARD", libelle: "En retard" },
];

/** Durée estimée du catalogue (minutes) en texte lisible. */
function duree(minutes: number): string {
  if (minutes < 60) return `${minutes} min`;
  if (minutes < 1440) return minutes % 60 === 0 ? `${minutes / 60} h` : `${Math.floor(minutes / 60)} h ${minutes % 60} min`;
  const jours = Math.round((minutes / 1440) * 10) / 10;
  return `${jours} j`;
}

export function PagePlanification({ filtre, onFiltre }: {
  filtre: FiltreAnalyses;
  onFiltre: (filtre: FiltreAnalyses) => void;
}) {
  const queryClient = useQueryClient();
  const [selection, setSelection] = useState<number[]>([]);
  const [dialogue, setDialogue] = useState(false);
  const [recherche, setRecherche] = useState("");
  const [domaine, setDomaine] = useState("");
  const [technicien, setTechnicien] = useState("");

  const analyses = useQuery({
    queryKey: ["responsable-analyses", filtre],
    queryFn: () => listerAnalyses(filtre),
  });
  useEffect(() => setSelection([]), [filtre, domaine, technicien]);
  useEffect(() => setTechnicien(""), [filtre]);

  const domaines = useMemo(() => {
    const parCode = new Map<string, string>();
    for (const a of analyses.data ?? []) if (a.domaineCode) parCode.set(a.domaineCode, a.domaineLibelle ?? a.domaineCode);
    return [...parCode.entries()].sort((x, y) => x[1].localeCompare(y[1], "fr"));
  }, [analyses.data]);
  const techniciensListes = useMemo(() => {
    const parId = new Map<number, string>();
    for (const a of analyses.data ?? []) if (a.technicienId != null) parId.set(a.technicienId, a.technicienNom ?? `#${a.technicienId}`);
    return [...parId.entries()].sort((x, y) => x[1].localeCompare(y[1], "fr"));
  }, [analyses.data]);

  // Le serveur renvoie les demandes les plus récentes d'abord ; on garde cet ordre en regroupant.
  const groupes = useMemo(() => {
    const parDemande = new Map<number, AnalyseTravail[]>();
    (analyses.data ?? [])
      .filter((a) => !domaine || a.domaineCode === domaine)
      .filter((a) => !technicien || String(a.technicienId) === technicien)
      .filter((a) => !recherche.trim()
        || contient(a.demandeReference, recherche)
        || contient(a.demandeTitre, recherche)
        || contient(a.essaiLibelle, recherche)
        || contient(a.technicienNom, recherche))
      .forEach((analyse) => {
        parDemande.set(analyse.demandeId, [...(parDemande.get(analyse.demandeId) ?? []), analyse]);
      });
    return [...parDemande.values()];
  }, [analyses.data, recherche, domaine, technicien]);

  const choisies = (analyses.data ?? []).filter((analyse) => selection.includes(analyse.id));
  const basculer = (id: number, coche: boolean) =>
    setSelection((courant) => (coche ? [...new Set([...courant, id])] : courant.filter((element) => element !== id)));

  return (
    <Container className="py-8">
      <EnTeteResponsable
        icone={CalendarClock}
        titre="Planification"
        description="Affectez les analyses des demandes acceptées aux techniciens."
        action={
          <Actualisation
            misAJour={analyses.dataUpdatedAt}
            enCours={analyses.isFetching}
            onActualiser={() => void analyses.refetch()}
          />
        }
      />

      <BarreFiltres>
        <div className="flex shrink-0 gap-1 rounded-xl bg-surface-alt p-1" role="tablist" aria-label="Filtrer les analyses">
          {ONGLETS.map((onglet) => (
            <button
              key={onglet.id}
              type="button"
              role="tab"
              aria-selected={filtre === onglet.id}
              onClick={() => onFiltre(onglet.id)}
              className={`flex-1 rounded-lg px-3 py-1.5 text-sm font-semibold whitespace-nowrap focus-visible:outline-2 focus-visible:outline-brand-900 lg:flex-none ${
                filtre === onglet.id ? "bg-brand-900 text-primary-foreground shadow-sm" : "text-brand-900 hover:bg-card"
              }`}
            >
              {onglet.libelle}
            </button>
          ))}
        </div>
        <Recherche className="w-full lg:flex-1" valeur={recherche} onChange={setRecherche} placeholder="Rechercher une demande, une analyse, un technicien…" />
        <FiltreSelect className="w-full lg:w-44" libelle="Filtrer par domaine" tous="Tous les domaines" valeur={domaine} onChange={setDomaine}
          options={domaines} />
        {filtre !== "A_AFFECTER" && (
          <FiltreSelect className="w-full lg:w-48" libelle="Filtrer par technicien" tous="Tous les techniciens" valeur={technicien}
            onChange={setTechnicien} options={techniciensListes.map(([id, nom]) => [String(id), nom])} />
        )}
        <Button
          className="h-10 shrink-0"
          icon={<UserCheck className="size-4" aria-hidden="true" />}
          iconPosition="left"
          disabled={selection.length === 0}
          onClick={() => setDialogue(true)}
        >
          {filtre === "A_AFFECTER" ? "Affecter" : "Réaffecter"}{selection.length > 0 ? ` (${selection.length})` : ""}
        </Button>
      </BarreFiltres>

      <EtatChargement
        chargement={analyses.isLoading}
        erreur={analyses.isError}
        vide={!analyses.isLoading && groupes.length === 0}
        messageVide={recherche.trim() || domaine || technicien ? "Aucune analyse ne correspond aux filtres."
          : filtre === "A_AFFECTER" ? "Toutes les analyses sont affectées."
            : filtre === "EN_RETARD" ? "Aucune analyse en retard." : "Aucune analyse affectée en cours."}
        onReessayer={() => void analyses.refetch()}
      />

      <div className="flex flex-col gap-4">
        {groupes.map((groupe) => {
          const premiere = groupe[0];
          if (!premiere) return null;
          const tousCoches = groupe.every((analyse) => selection.includes(analyse.id));
          return (
            <section key={premiere.demandeId} className="overflow-hidden rounded-2xl border border-brand-900/10 bg-card">
              <header className="flex flex-wrap items-center justify-between gap-2 border-b border-brand-900/10 bg-surface-alt px-4 py-3">
                <label className="flex items-center gap-3 text-sm font-semibold text-ink-900">
                  <input
                    type="checkbox"
                    className="size-4 accent-brand-900"
                    checked={tousCoches}
                    aria-label={`Sélectionner toutes les analyses de ${premiere.demandeReference}`}
                    onChange={(event) => groupe.forEach((analyse) => basculer(analyse.id, event.target.checked))}
                  />
                  {premiere.demandeReference}
                  {premiere.demandeTitre && <span className="font-normal text-muted-foreground">· {premiere.demandeTitre}</span>}
                </label>
                <div className="flex items-center gap-3 text-xs text-muted-foreground">
                  <span>Reçue le {formaterDate(premiere.demandeDateSoumission)} · {groupe.length} analyse(s)</span>
                  <Link
                    to="/app/demandes-clients"
                    search={{ page: 1, tri: "dateSoumission", direction: "desc", demande: premiere.demandeId }}
                    className="font-semibold text-brand-900 underline-offset-4 hover:underline"
                  >
                    Voir la demande
                  </Link>
                </div>
              </header>
              <ul>
                {groupe.map((analyse) => (
                  <li key={analyse.id} className="flex flex-wrap items-center gap-3 border-t border-brand-900/5 px-4 py-3 first:border-t-0">
                    <input
                      type="checkbox"
                      className="size-4 accent-brand-900"
                      checked={selection.includes(analyse.id)}
                      aria-label={`Sélectionner ${analyse.essaiLibelle ?? analyse.code}`}
                      onChange={(event) => basculer(analyse.id, event.target.checked)}
                    />
                    <div className="min-w-0 flex-1">
                      <p className="font-semibold text-ink-900">{analyse.essaiLibelle ?? analyse.code}</p>
                      <p className="text-xs text-muted-foreground">
                        {analyse.domaineLibelle ?? "Sans domaine"}
                        {analyse.dureeEstimeeMinutes != null ? ` · ${duree(analyse.dureeEstimeeMinutes)} estimé(s)` : ""}
                        {analyse.technicienNom ? ` · ${analyse.technicienNom}` : ""}
                        {analyse.dateAttribution ? ` · affectée le ${formaterDate(analyse.dateAttribution)}` : ""}
                        {analyse.echeance ? ` · échéance ${formaterDate(analyse.echeance)}` : ""}
                      </p>
                    </div>
                    <BadgeStatutAnalyse analyse={analyse} />
                  </li>
                ))}
              </ul>
            </section>
          );
        })}
      </div>

      <DialogueAffectation
        ouvert={dialogue}
        analyses={choisies}
        onFermer={() => setDialogue(false)}
        onAffecte={async () => {
          setDialogue(false);
          setSelection([]);
          await queryClient.invalidateQueries({ queryKey: ["responsable-analyses"] });
          await queryClient.invalidateQueries({ queryKey: ["responsable-suivi"] });
          await queryClient.invalidateQueries({ queryKey: ["responsable-techniciens"] });
        }}
      />
    </Container>
  );
}

function DialogueAffectation({ ouvert, analyses, onFermer, onAffecte }: {
  ouvert: boolean;
  analyses: AnalyseTravail[];
  onFermer: () => void;
  onAffecte: () => Promise<void>;
}) {
  const [technicienId, setTechnicienId] = useState<number | null>(null);
  const [datePrevue, setDatePrevue] = useState("");
  const [motif, setMotif] = useState("");
  const techniciens = useQuery({ queryKey: ["responsable-techniciens"], queryFn: listerTechniciens, enabled: ouvert });

  useEffect(() => {
    if (ouvert) {
      setTechnicienId(null);
      setDatePrevue("");
      setMotif("");
    }
  }, [ouvert]);

  // Réaffectation : une des analyses est déjà confiée à un autre technicien que celui choisi.
  const reaffectees = technicienId == null ? [] : analyses.filter((a) => a.technicienId != null && a.technicienId !== technicienId);
  const motifManquant = reaffectees.length > 0 && !motif.trim();
  const affectation = useMutation({
    mutationFn: () => affecterAnalyses(analyses.map((a) => a.id), technicienId as number, datePrevue || undefined,
      reaffectees.length > 0 ? motif.trim() : undefined),
    onSuccess: async () => {
      toast.success(`${analyses.length} analyse(s) affectée(s).`);
      await onAffecte();
    },
    onError: (erreur) => toast.error(messageErreur(erreur, "L'affectation a échoué.")),
  });

  return (
    <Dialogue
      ouvert={ouvert}
      titre={`Affecter ${analyses.length} analyse(s)`}
      description="Les techniciens les moins chargés sont proposés en premier. Seuls ceux qui couvrent le domaine des analyses peuvent être choisis."
      onFermer={onFermer}
    >
      <div className="flex flex-col gap-4 text-sm">
        {techniciens.isLoading && <p className="text-muted-foreground">Chargement des techniciens…</p>}
        {techniciens.data?.length === 0 && (
          <p className="rounded-2xl bg-surface-alt px-4 py-3 text-ink-900">Aucun technicien actif dans le laboratoire.</p>
        )}
        <fieldset className="flex max-h-72 flex-col gap-2 overflow-y-auto">
          <legend className="sr-only">Technicien</legend>
          {techniciens.data?.map((technicien) => {
            const sansDomaine = technicien.domaines.length === 0;
            const couvre = !sansDomaine && analyses.every((a) => a.domaineCode != null && technicien.domaines.includes(a.domaineCode));
            return (
              <label
                key={technicien.id}
                className={`flex items-center gap-3 rounded-2xl border px-4 py-3 ${
                  !couvre ? "cursor-not-allowed opacity-50" : technicienId === technicien.id ? "border-brand-900 bg-brand-900/5" : "border-brand-900/10"
                }`}
              >
                <input
                  type="radio"
                  name="technicien"
                  className="size-4 accent-brand-900"
                  disabled={!couvre}
                  checked={technicienId === technicien.id}
                  onChange={() => setTechnicienId(technicien.id)}
                />
                <div className="min-w-0 flex-1">
                  <p className="font-semibold text-ink-900">{technicien.nomComplet}</p>
                  <p className="text-xs text-muted-foreground">
                    {sansDomaine ? "Aucun domaine attribué" : technicien.domaines.join(", ")}
                    {!couvre && !sansDomaine ? " · domaine non couvert" : ""}
                  </p>
                </div>
                <div className="flex gap-1">
                  <Badge size="sm" variant="outline">{technicien.aFaire + technicien.enCours} en charge</Badge>
                  {technicien.enRetard > 0 && <Badge size="sm" variant="dark">{technicien.enRetard} en retard</Badge>}
                </div>
              </label>
            );
          })}
        </fieldset>
        <div className="flex max-w-xs flex-col gap-1.5">
          <Label htmlFor="date-prevue">Début prévu (facultatif)</Label>
          <Input id="date-prevue" type="date" min={new Date().toLocaleDateString("en-CA", { timeZone: "Africa/Casablanca" })} value={datePrevue} onChange={(event) => setDatePrevue(event.target.value)} />
        </div>
        {reaffectees.length > 0 && (
          <div className="flex flex-col gap-1.5 rounded-2xl border border-amber-200 bg-amber-50 p-3">
            <Label htmlFor="motif-reaffectation">Motif de la réaffectation (obligatoire)</Label>
            <p className="text-xs text-amber-900">
              {reaffectees.length} analyse(s) déjà confiée(s) à {[...new Set(reaffectees.map((a) => a.technicienNom ?? "un autre technicien"))].join(", ")}.
              Le motif est inscrit au journal d'audit.
            </p>
            <Textarea id="motif-reaffectation" rows={2} maxLength={500} value={motif} onChange={(event) => setMotif(event.target.value)} />
          </div>
        )}
        <div className="flex justify-end gap-2">
          <Button variant="ghost" onClick={onFermer}>Annuler</Button>
          <Button loading={affectation.isPending} disabled={technicienId == null || motifManquant} onClick={() => affectation.mutate()}>
            Affecter
          </Button>
        </div>
      </div>
    </Dialogue>
  );
}
