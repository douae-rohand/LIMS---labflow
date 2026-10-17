import { Link } from "@tanstack/react-router";
import { Card } from "@/components/lab";
import { StatutDemandeBadge } from "@/components/demandes/StatutDemandeBadge";
import { Container } from "@/components/layout/Container";
import { CardsSkeleton, LandingState } from "@/components/landing/LandingState";
import { useDemandesClient } from "@/hooks/useDemandesClient";
import type { StatutDemande } from "@/api/demandes";
import type { DemandeResumeClient } from "@/api/demandesClient";
import { formatDate } from "./libelles";

/** Statuts regroupés sous « en cours » dans le tableau de bord. */
const STATUTS_EN_COURS: StatutDemande[] = ["ACCEPTEE", "EN_COURS", "EN_VALIDATION"];
const NB_DERNIERES = 5;

interface Statistique {
  libelle: string;
  valeur: number;
}

export function TableauBordClient() {
  const demandesQuery = useDemandesClient();
  const demandes = demandesQuery.data ?? [];

  const enAttente = demandes.filter((demande) => demande.statut === "NOUVEAU").length;
  const enCours = demandes.filter((demande) =>
    STATUTS_EN_COURS.includes(demande.statut),
  ).length;
  const terminees = demandes.filter((demande) => demande.statut === "TERMINEE").length;

  const statistiques: Statistique[] = [
    { libelle: "Demandes en attente de traitement", valeur: enAttente },
    { libelle: "Demandes en cours de traitement", valeur: enCours },
    { libelle: "Demandes terminées", valeur: terminees },
    { libelle: "Total des demandes", valeur: demandes.length },
  ];

  return (
    <Container className="py-10">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-ink-900">Tableau de bord</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Suivez l&apos;avancement de vos demandes d&apos;analyses.
          </p>
        </div>
        <Link
          to="/app/demandes"
          className="text-sm font-semibold text-brand-900 hover:underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-900"
        >
          Voir toutes mes demandes
        </Link>
      </div>

      <div className="mt-8">
        <LandingState
          loading={demandesQuery.isPending}
          error={demandesQuery.error}
          onRetry={() => {
            void demandesQuery.refetch();
          }}
          empty={demandes.length === 0}
          emptyMessage="Vous n'avez encore envoyé aucune demande. Parcourez le catalogue d'un laboratoire pour commencer."
          skeleton={<CardsSkeleton count={4} />}
        >
          <div className="flex flex-col gap-8">
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
              {statistiques.map((statistique) => (
                <Card key={statistique.libelle} variant="surface" padding="md">
                  <p className="text-3xl font-extrabold text-brand-900">{statistique.valeur}</p>
                  <p className="mt-1 text-sm text-muted-foreground">{statistique.libelle}</p>
                </Card>
              ))}
            </div>

            <div>
              <h2 className="text-lg font-extrabold text-ink-900">Dernières demandes</h2>
              <ul className="mt-4 flex flex-col gap-3">
                {demandes.slice(0, NB_DERNIERES).map((demande) => (
                  <li key={demande.id}>
                    <LigneDerniereDemande demande={demande} />
                  </li>
                ))}
              </ul>
            </div>
          </div>
        </LandingState>
      </div>
    </Container>
  );
}

function LigneDerniereDemande({ demande }: { demande: DemandeResumeClient }) {
  const contenu = (
    <>
      <div className="min-w-0">
        <p className="font-mono text-xs text-muted-foreground">{demande.reference}</p>
        <p className="mt-0.5 truncate text-sm font-semibold text-ink-900">
          {demande.objet ?? "Demande"}
        </p>
        <p className="mt-0.5 text-xs text-muted-foreground">
          {demande.laboratoireNom ?? demande.laboratoireCode ?? "Laboratoire non précisé"}
          {" — "}
          {formatDate(demande.dateSoumission)}
        </p>
      </div>
      <StatutDemandeBadge statut={demande.statut} />
    </>
  );

  const classes =
    "flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-brand-900/10 bg-card px-4 py-3";

  if (demande.laboratoireId === null) {
    return <div className={classes}>{contenu}</div>;
  }

  return (
    <Link
      to="/app/demandes/$laboratoireId/$demandeId"
      params={{
        laboratoireId: String(demande.laboratoireId),
        demandeId: String(demande.id),
      }}
      className={`${classes} transition-colors hover:bg-surface-card focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-900`}
    >
      {contenu}
    </Link>
  );
}
