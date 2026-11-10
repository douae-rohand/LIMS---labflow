import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { ClipboardList } from "lucide-react";
import { Badge, Button, Select } from "@/components/lab";
import { Container } from "@/components/layout/Container";
import { LIBELLE_STATUT_DEMANDE, STATUTS_DEMANDE, type StatutDemande } from "@/api/demandes";
import { lireSuiviDemande, listerDemandesSuivi } from "@/api/accueil";
import { Panneau } from "@/features/super-admin/Fenetres";
import {
  Actualisation,
  EnTeteResponsable,
  EtatChargement,
  formaterDate,
  INTERVALLE_ACTUALISATION,
  Recherche,
} from "@/features/responsable/commun";
import { formaterMontant } from "./commun";

const VARIANTE_STATUT: Record<StatutDemande, "lime" | "mint" | "soft" | "dark" | "outline"> = {
  NOUVEAU: "lime",
  ACCEPTEE: "soft",
  EN_COURS: "soft",
  EN_VALIDATION: "soft",
  TERMINEE: "mint",
  ANNULEE: "outline",
  REFUSEE: "dark",
};

const LIBELLE_ANALYSE: Record<string, string> = {
  ATTENTE_ECHANTILLON: "En attente d'échantillon",
  EN_COURS: "En cours",
  TERMINE: "Résultat saisi",
  NON_REALISABLE: "Non réalisable",
};

function BadgeStatut({ statut }: { statut: StatutDemande }) {
  return <Badge size="sm" variant={VARIANTE_STATUT[statut] ?? "outline"}>{LIBELLE_STATUT_DEMANDE[statut] ?? statut}</Badge>;
}

export function PageSuiviDemandes() {
  const [saisie, setSaisie] = useState("");
  const [recherche, setRecherche] = useState("");
  const [statut, setStatut] = useState<StatutDemande | "">("");
  const [page, setPage] = useState(0);
  const [choisie, setChoisie] = useState<number | null>(null);

  // Recherche côté serveur, déclenchée après une courte pause de saisie.
  useEffect(() => {
    const minuteur = window.setTimeout(() => { setRecherche(saisie.trim()); setPage(0); }, 350);
    return () => window.clearTimeout(minuteur);
  }, [saisie]);

  const demandes = useQuery({
    queryKey: ["accueil-demandes", recherche, statut, page],
    queryFn: () => listerDemandesSuivi({ q: recherche, statut: statut || undefined, page }),
    placeholderData: keepPreviousData,
    refetchInterval: INTERVALLE_ACTUALISATION,
  });
  const liste = demandes.data?.content ?? [];

  return (
    <Container className="py-8">
      <EnTeteResponsable
        icone={ClipboardList}
        titre="Suivi des demandes"
        description="Renseignez les clients au guichet : statut, échantillons, analyses, facture et rapport. Consultation seule."
        action={
          <Actualisation misAJour={demandes.dataUpdatedAt} enCours={demandes.isFetching}
            onActualiser={() => void demandes.refetch()} />
        }
      />

      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <Select aria-label="Filtrer par statut" className="sm:w-56" value={statut}
          onChange={(event) => { setStatut(event.target.value as StatutDemande | ""); setPage(0); }}>
          <option value="">Tous les statuts</option>
          {STATUTS_DEMANDE.map((s) => <option key={s} value={s}>{LIBELLE_STATUT_DEMANDE[s]}</option>)}
        </Select>
        <Recherche valeur={saisie} onChange={setSaisie} placeholder="Rechercher une référence, un client…" />
      </div>

      <EtatChargement
        chargement={demandes.isLoading}
        erreur={demandes.isError}
        vide={!demandes.isLoading && liste.length === 0}
        messageVide={recherche || statut ? "Aucune demande ne correspond." : "Aucune demande pour le moment."}
        onReessayer={() => void demandes.refetch()}
      />

      <ul className="flex flex-col gap-3">
        {liste.map((d) => (
          <li key={d.id}>
            <button type="button" onClick={() => setChoisie(d.id)}
              className="flex w-full flex-wrap items-center justify-between gap-3 rounded-2xl border border-brand-900/10 bg-card px-4 py-4 text-left hover:bg-surface-alt">
              <div className="min-w-0">
                <div className="flex flex-wrap items-center gap-2">
                  <p className="font-semibold text-ink-900">{d.reference}</p>
                  <BadgeStatut statut={d.statut} />
                </div>
                <p className="text-xs text-muted-foreground">
                  {d.demandeur ?? "Client"}{d.objet ? ` · ${d.objet}` : ""} · soumise le {formaterDate(d.dateSoumission, true)}
                </p>
              </div>
              <span className="text-sm font-semibold text-brand-900">Voir le suivi</span>
            </button>
          </li>
        ))}
      </ul>

      {(demandes.data?.totalPages ?? 0) > 1 && (
        <div className="mt-4 flex items-center justify-end gap-2 text-sm">
          <Button size="sm" variant="ghost" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>Précédent</Button>
          <span className="text-muted-foreground">Page {page + 1} / {demandes.data?.totalPages}</span>
          <Button size="sm" variant="ghost" disabled={page + 1 >= (demandes.data?.totalPages ?? 1)} onClick={() => setPage((p) => p + 1)}>Suivant</Button>
        </div>
      )}

      <PanneauSuivi demandeId={choisie} onFermer={() => setChoisie(null)} />
    </Container>
  );
}

function PanneauSuivi({ demandeId, onFermer }: { demandeId: number | null; onFermer: () => void }) {
  const suivi = useQuery({
    queryKey: ["accueil-suivi", demandeId],
    queryFn: () => lireSuiviDemande(demandeId as number),
    enabled: demandeId != null,
  });
  const s = suivi.data;
  const d = s?.demande;

  return (
    <Panneau ouvert={demandeId != null} titre={`Suivi · ${d?.reference ?? ""}`} onFermer={onFermer} large>
      <EtatChargement chargement={suivi.isLoading} erreur={suivi.isError} vide={false} messageVide=""
        onReessayer={() => void suivi.refetch()} />
      {s && d && (
        <div className="flex flex-col gap-6 text-sm">
          <div className="flex flex-wrap items-center gap-2">
            <BadgeStatut statut={d.statut} />
            <span className="text-muted-foreground">
              {d.client?.raisonSociale ?? d.client?.nomComplet ?? "Client"}
              {d.patient?.nomComplet ? ` · patient ${d.patient.nomComplet}` : ""}
              {d.objet ? ` · ${d.objet}` : ""}
            </span>
          </div>
          {d.motif && (d.statut === "REFUSEE" || d.statut === "ANNULEE") && (
            <p className="rounded-2xl bg-red-50 px-4 py-3 text-red-800">Motif : {d.motif}</p>
          )}

          <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
            <Tuile titre="Analyses" valeur={`${s.avancement.terminees}/${s.avancement.analyses} terminées`}
              detail={`${s.avancement.attenteEchantillon} en attente d'échantillon · ${s.avancement.enCours} en cours`} />
            <Tuile titre="Échantillons" valeur={`${d.echantillons.filter((e) => e.conformite !== false).length} conforme(s)`}
              detail={`${d.echantillons.length} reçu(s) au total`} />
            <Tuile titre="Facture" valeur={s.facture ? (s.facture.reste === 0 ? "Réglée" : `Reste ${formaterMontant(s.facture.reste)}`) : "Pas encore émise"}
              detail={s.facture ? `${s.facture.numero} · ${formaterMontant(s.facture.montant)}` : "Émise à l'acceptation"} />
            <Tuile titre="Rapport" valeur={s.rapport ? (s.rapport.diffuse ? "Disponible pour le client" : "En préparation") : "Pas encore produit"}
              detail={s.rapport ? `${s.rapport.numero}${s.rapport.dateSignature ? ` · signé le ${formaterDate(s.rapport.dateSignature)}` : ""}` : "Après validation des résultats"} />
          </div>

          <section>
            <h3 className="mb-2 font-bold text-ink-900">Analyses</h3>
            <ul className="flex flex-col gap-2">
              {d.analyses.map((a) => (
                <li key={a.id} className="flex flex-wrap items-center justify-between gap-2 rounded-xl border border-brand-900/10 px-3 py-2">
                  <span className="text-ink-900">{a.designation ?? a.code}<span className="text-xs text-muted-foreground"> · {a.code}</span></span>
                  <Badge size="sm" variant={a.statut === "TERMINE" ? "mint" : "outline"}>
                    {LIBELLE_ANALYSE[a.statut ?? "ATTENTE_ECHANTILLON"] ?? a.statut}
                  </Badge>
                </li>
              ))}
            </ul>
          </section>

          <section>
            <h3 className="mb-2 font-bold text-ink-900">Échantillons</h3>
            {d.echantillons.length === 0 ? (
              <p className="text-xs text-muted-foreground">Aucun échantillon reçu.</p>
            ) : (
              <ul className="flex flex-col gap-2">
                {d.echantillons.map((e) => (
                  <li key={e.id} className="flex flex-wrap items-center justify-between gap-2 rounded-xl border border-brand-900/10 px-3 py-2">
                    <span className="text-ink-900">{e.reference}{e.nature ? ` · ${e.nature}` : ""}
                      <span className="text-xs text-muted-foreground"> · reçu le {formaterDate(e.dateReception, true)}</span></span>
                    <Badge size="sm" variant={e.conformite === false ? "dark" : "mint"}>{e.conformite === false ? "Non conforme" : "Conforme"}</Badge>
                  </li>
                ))}
              </ul>
            )}
          </section>

          <section>
            <h3 className="mb-2 font-bold text-ink-900">Historique</h3>
            {s.historique.length === 0 ? (
              <p className="text-xs text-muted-foreground">Aucun événement enregistré.</p>
            ) : (
              <ol className="relative flex flex-col gap-3 border-l border-brand-900/15 pl-4">
                {s.historique.map((etape, index) => (
                  <li key={`${etape.date}-${index}`}>
                    <span className="absolute -left-1.5 mt-1.5 size-3 rounded-full bg-brand-900" aria-hidden="true" />
                    <p className="font-semibold text-ink-900">{etape.libelle}</p>
                    <p className="text-xs text-muted-foreground">{formaterDate(etape.date, true)} · {etape.acteur}</p>
                    {etape.motif && <p className="text-xs text-ink-900">Motif : {etape.motif}</p>}
                  </li>
                ))}
              </ol>
            )}
          </section>
        </div>
      )}
    </Panneau>
  );
}

function Tuile({ titre, valeur, detail }: { titre: string; valeur: string; detail: string }) {
  return (
    <div className="rounded-2xl border border-brand-900/10 bg-card px-4 py-3">
      <p className="text-xs text-muted-foreground">{titre}</p>
      <p className="font-bold text-ink-900">{valeur}</p>
      <p className="text-xs text-muted-foreground">{detail}</p>
    </div>
  );
}
