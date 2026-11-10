import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useMemo, useState } from "react";
import { CalendarClock, UserCheck } from "lucide-react";
import { toast } from "sonner";
import { Badge, Button, Input, Label } from "@/components/lab";
import { Container } from "@/components/layout/Container";
import {
  affecterAnalyses,
  listerAnalyses,
  listerTechniciens,
  type AnalyseTravail,
  type FiltreAnalyses,
} from "@/api/responsable";
import { Dialogue } from "@/features/super-admin/Fenetres";
import { BadgeStatutAnalyse, EnTeteResponsable, EtatChargement, formaterDate, messageErreur } from "./commun";

const ONGLETS: { id: FiltreAnalyses; libelle: string }[] = [
  { id: "A_AFFECTER", libelle: "À affecter" },
  { id: "AFFECTEES", libelle: "Affectées" },
  { id: "EN_RETARD", libelle: "En retard" },
];

export function PagePlanification() {
  const queryClient = useQueryClient();
  const [filtre, setFiltre] = useState<FiltreAnalyses>("A_AFFECTER");
  const [selection, setSelection] = useState<number[]>([]);
  const [dialogue, setDialogue] = useState(false);

  const analyses = useQuery({ queryKey: ["responsable-analyses", filtre], queryFn: () => listerAnalyses(filtre) });
  useEffect(() => setSelection([]), [filtre]);

  // Regroupe les analyses par demande pour une lecture « dossier ».
  const groupes = useMemo(() => {
    const parDemande = new Map<number, AnalyseTravail[]>();
    (analyses.data ?? []).forEach((analyse) => {
      parDemande.set(analyse.demandeId, [...(parDemande.get(analyse.demandeId) ?? []), analyse]);
    });
    return [...parDemande.values()];
  }, [analyses.data]);

  const choisies = (analyses.data ?? []).filter((analyse) => selection.includes(analyse.id));
  const basculer = (id: number, coche: boolean) =>
    setSelection((courant) => (coche ? [...courant, id] : courant.filter((element) => element !== id)));

  return (
    <Container className="py-8">
      <EnTeteResponsable
        icone={CalendarClock}
        titre="Planification"
        description="Affectez les analyses des demandes acceptées aux techniciens de votre laboratoire."
        action={
          <Button
            icon={<UserCheck className="size-4" aria-hidden="true" />}
            iconPosition="left"
            disabled={selection.length === 0}
            onClick={() => setDialogue(true)}
          >
            Affecter {selection.length > 0 ? `(${selection.length})` : ""}
          </Button>
        }
      />

      <div className="mb-4 inline-flex w-full gap-1 rounded-2xl border border-brand-900/10 bg-card p-1 sm:w-auto" role="tablist" aria-label="Filtrer les analyses">
        {ONGLETS.map((onglet) => (
          <button
            key={onglet.id}
            type="button"
            role="tab"
            aria-selected={filtre === onglet.id}
            onClick={() => setFiltre(onglet.id)}
            className={`flex-1 rounded-xl px-4 py-2 text-sm font-semibold sm:flex-none ${
              filtre === onglet.id ? "bg-brand-900 text-primary-foreground" : "text-brand-900 hover:bg-surface-alt"
            }`}
          >
            {onglet.libelle}
          </button>
        ))}
      </div>

      <EtatChargement
        chargement={analyses.isLoading}
        erreur={analyses.isError}
        vide={!analyses.isLoading && (analyses.data?.length ?? 0) === 0}
        messageVide={filtre === "A_AFFECTER" ? "Toutes les analyses sont affectées." : filtre === "EN_RETARD" ? "Aucune analyse en retard." : "Aucune analyse affectée en cours."}
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
                <span className="text-xs text-muted-foreground">{groupe.length} analyse(s)</span>
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
                        {analyse.dureeEstimeeJours != null ? ` · ${analyse.dureeEstimeeJours} j estimé(s)` : ""}
                        {analyse.technicienNom ? ` · ${analyse.technicienNom}` : ""}
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
  const techniciens = useQuery({ queryKey: ["responsable-techniciens"], queryFn: listerTechniciens, enabled: ouvert });

  useEffect(() => {
    if (ouvert) {
      setTechnicienId(null);
      setDatePrevue("");
    }
  }, [ouvert]);

  const domainesRequis = [...new Set(analyses.map((a) => a.domaineCode).filter((code): code is string => Boolean(code)))];
  const affectation = useMutation({
    mutationFn: () => affecterAnalyses(analyses.map((a) => a.id), technicienId as number, datePrevue || undefined),
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
      description="Choisissez un technicien. Ceux qui ne couvrent pas le domaine des analyses ne peuvent pas être choisis."
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
            const couvre = technicien.domaines.length === 0 || domainesRequis.every((d) => technicien.domaines.includes(d));
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
                    {technicien.domaines.join(", ") || "Tous domaines"}
                    {!couvre ? " · domaine non couvert" : ""}
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
          <Input id="date-prevue" type="date" min={new Date().toISOString().slice(0, 10)} value={datePrevue} onChange={(event) => setDatePrevue(event.target.value)} />
        </div>
        <div className="flex justify-end gap-2">
          <Button variant="ghost" onClick={onFermer}>Annuler</Button>
          <Button loading={affectation.isPending} disabled={technicienId == null} onClick={() => affectation.mutate()}>
            Affecter
          </Button>
        </div>
      </div>
    </Dialogue>
  );
}
