import { useQuery } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { PackageCheck } from "lucide-react";
import { Button } from "@/components/lab";
import { Container } from "@/components/layout/Container";
import { listerReceptions, type DemandeReception } from "@/api/accueil";
import {
  Actualisation,
  contient,
  EnTeteResponsable,
  BarreFiltres,
  EtatChargement,
  FiltreSelect,
  formaterDate,
  Onglets,
  Recherche,
} from "@/features/responsable/commun";
import { BadgeEtatReception, demandeur } from "./commun";
import { PanneauReception } from "./PanneauReception";

type Filtre = "a-recevoir" | "partielles" | "recues" | "toutes";

const FILTRES: { id: Filtre; libelle: string; garde: (d: DemandeReception) => boolean }[] = [
  { id: "a-recevoir", libelle: "À réceptionner", garde: (d) => d.etat !== "COMPLETE" },
  { id: "partielles", libelle: "Partielles", garde: (d) => d.etat === "PARTIELLE" },
  { id: "recues", libelle: "Reçues", garde: (d) => d.etat === "COMPLETE" },
  { id: "toutes", libelle: "Toutes", garde: () => true },
];

export function PageReception({ demandeInitiale, onDemande }: {
  demandeInitiale: number | undefined;
  onDemande: (demandeId?: number) => void;
}) {
  const [filtre, setFiltre] = useState<Filtre>("a-recevoir");
  const [recherche, setRecherche] = useState("");
  const [demandeurFiltre, setDemandeurFiltre] = useState("");
  const receptions = useQuery({
    queryKey: ["accueil-receptions"],
    queryFn: listerReceptions,
  });

  const compteurs = useMemo(() => Object.fromEntries(FILTRES.map((f) =>
    [f.id, (receptions.data ?? []).filter(f.garde).length])) as Record<Filtre, number>, [receptions.data]);
  const garde = FILTRES.find((f) => f.id === filtre)?.garde ?? (() => true);
  // Le serveur trie : la dernière activité (réception ou acceptation) en haut.
  const visibles = (receptions.data ?? [])
    .filter(garde)
    .filter((d) => !demandeurFiltre || (demandeurFiltre === "PATIENT" ? !d.client && !!d.patient : !!d.client))
    .filter((d) => !recherche.trim()
      || contient(d.reference, recherche)
      || contient(d.client, recherche)
      || contient(d.patient, recherche)
      || contient(d.titre, recherche));

  return (
    <Container className="py-8">
      <EnTeteResponsable
        icone={PackageCheck}
        titre="Réception des échantillons"
        description="Enregistrez les échantillons déposés pour les demandes acceptées : chaque échantillon conforme débloque les analyses du technicien."
        action={
          <Actualisation
            misAJour={receptions.dataUpdatedAt}
            enCours={receptions.isFetching}
            onActualiser={() => void receptions.refetch()}
          />
        }
      />

      <BarreFiltres>
        <Onglets libelle="Filtrer les demandes" onglets={FILTRES} valeur={filtre} onChange={setFiltre} compteurs={compteurs} />
        <Recherche className="w-full lg:flex-1" valeur={recherche} onChange={setRecherche} placeholder="Rechercher une demande, un client, un patient…" />
        <FiltreSelect className="w-full lg:w-44" libelle="Filtrer par demandeur" tous="Clients et patients" valeur={demandeurFiltre}
          onChange={setDemandeurFiltre} options={[["CLIENT", "Clients"], ["PATIENT", "Patients sur site"]]} />
      </BarreFiltres>

      <EtatChargement
        chargement={receptions.isLoading}
        erreur={receptions.isError}
        vide={!receptions.isLoading && visibles.length === 0}
        messageVide={(receptions.data?.length ?? 0) === 0 ? "Aucune demande acceptée pour le moment." : "Aucune demande ne correspond."}
        onReessayer={() => void receptions.refetch()}
      />

      <ul className="flex flex-col gap-3">
        {visibles.map((demande) => (
          <li key={demande.demandeId} className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-brand-900/10 bg-card px-4 py-4">
            <div className="min-w-0 flex-1">
              <div className="flex flex-wrap items-center gap-2">
                <p className="font-semibold text-ink-900">{demande.reference}</p>
                <BadgeEtatReception demande={demande} />
                {demande.nonConformes > 0 && (
                  <span className="rounded-full bg-red-50 px-2 py-0.5 text-[11px] font-semibold text-red-700">
                    {demande.nonConformes} non conforme(s)
                  </span>
                )}
              </div>
              <p className="text-xs text-muted-foreground">
                {demandeur(demande)}{demande.titre ? ` · ${demande.titre}` : ""}
              </p>
              <p className="mt-1 text-xs text-muted-foreground">
                {demande.recues}/{demande.analyses} analyse(s) couvertes
                {demande.derniereReception
                  ? ` · dernière réception ${formaterDate(demande.derniereReception, true)}`
                  : ` · acceptée le ${formaterDate(demande.dateDecision)}`}
              </p>
              <div className="mt-2 h-1.5 w-48 max-w-full overflow-hidden rounded-full bg-surface-alt" role="img"
                aria-label={`${demande.recues} sur ${demande.analyses} analyses couvertes`}>
                <div className="h-full rounded-full bg-brand-900" style={{ width: `${demande.analyses ? (demande.recues / demande.analyses) * 100 : 0}%` }} />
              </div>
            </div>
            <Button size="sm" variant={demande.enAttente > 0 ? "primary" : "secondary"} onClick={() => onDemande(demande.demandeId)}>
              {demande.enAttente > 0 ? "Réceptionner" : "Voir les échantillons"}
            </Button>
          </li>
        ))}
      </ul>

      <PanneauReception demandeId={demandeInitiale} onFermer={() => onDemande(undefined)} />
    </Container>
  );
}
