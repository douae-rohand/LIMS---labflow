import { useQuery } from "@tanstack/react-query";
import { Link } from "@tanstack/react-router";
import { AlertTriangle, Gauge, PackageCheck, PackageOpen, Receipt, Truck } from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { Badge, Card } from "@/components/lab";
import { Container } from "@/components/layout/Container";
import { lireTableauAccueil } from "@/api/accueil";
import {
  Actualisation,
  EnTeteResponsable,
  EtatChargement,
  formaterDate,
  INTERVALLE_ACTUALISATION,
} from "@/features/responsable/commun";
import { BadgeEtatReception, demandeur, formaterMontant } from "./commun";

export function TableauBordAccueil() {
  const tableau = useQuery({
    queryKey: ["accueil-tableau"],
    queryFn: lireTableauAccueil,
    refetchInterval: INTERVALLE_ACTUALISATION,
  });
  const donnees = tableau.data;

  return (
    <Container className="py-8">
      <EnTeteResponsable
        icone={Gauge}
        titre="Accueil"
        description="Échantillons à réceptionner, réceptions du jour et factures à encaisser."
        action={
          <Actualisation
            misAJour={tableau.dataUpdatedAt}
            enCours={tableau.isFetching}
            onActualiser={() => void tableau.refetch()}
          />
        }
      />

      <EtatChargement chargement={tableau.isLoading} erreur={tableau.isError} vide={false} messageVide=""
        onReessayer={() => void tableau.refetch()} />

      {donnees && (
        <div className="flex flex-col gap-6">
          <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
            <Indicateur icone={PackageOpen} libelle="Demandes à réceptionner" valeur={donnees.demandesARecevoir} lien />
            <Indicateur icone={Truck} libelle="Réceptions partielles" valeur={donnees.receptionsPartielles} lien />
            <Indicateur icone={PackageCheck} libelle="Échantillons reçus aujourd'hui" valeur={donnees.echantillonsRecusAujourdhui} />
            <Indicateur icone={AlertTriangle} libelle="Non conformes aujourd'hui" valeur={donnees.nonConformesAujourdhui} alerte />
          </div>

          <Card className="flex flex-wrap items-center gap-4 p-4">
            <span className="flex size-10 items-center justify-center rounded-xl bg-brand-900/10 text-brand-900">
              <Receipt className="size-5" aria-hidden="true" />
            </span>
            <div>
              <p className="text-sm text-muted-foreground">Factures à encaisser</p>
              <p className="text-lg font-extrabold text-ink-900">
                {donnees.facturesAEncaisser} facture(s) · reste {formaterMontant(donnees.resteAEncaisser)}
              </p>
            </div>
          </Card>

          <div className="grid gap-6 xl:grid-cols-2">
            <section>
              <div className="mb-3 flex items-center justify-between gap-2">
                <h2 className="text-lg font-bold text-ink-900">À réceptionner</h2>
                <Link to="/app/reception" className="text-sm font-semibold text-brand-900 underline-offset-4 hover:underline">
                  Tout voir
                </Link>
              </div>
              {donnees.prochainesReceptions.length === 0 ? (
                <EtatChargement chargement={false} erreur={false} vide messageVide="Aucun échantillon attendu." />
              ) : (
                <ul className="flex flex-col gap-2">
                  {donnees.prochainesReceptions.map((demande) => (
                    <li key={demande.demandeId}>
                      <Link to="/app/reception" search={{ demande: demande.demandeId }}
                        className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-brand-900/10 bg-card px-4 py-3 hover:bg-surface-alt">
                        <div className="min-w-0">
                          <p className="font-semibold text-ink-900">{demande.reference}</p>
                          <p className="text-xs text-muted-foreground">
                            {demandeur(demande)} · {demande.enAttente} analyse(s) en attente d'échantillon
                          </p>
                        </div>
                        <BadgeEtatReception demande={demande} />
                      </Link>
                    </li>
                  ))}
                </ul>
              )}
            </section>

            <section>
              <h2 className="mb-3 text-lg font-bold text-ink-900">Reçus aujourd'hui</h2>
              {donnees.dernieresReceptions.length === 0 ? (
                <EtatChargement chargement={false} erreur={false} vide messageVide="Aucun échantillon reçu aujourd'hui." />
              ) : (
                <ul className="flex flex-col gap-2">
                  {donnees.dernieresReceptions.map((echantillon) => (
                    <li key={echantillon.id} className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-brand-900/10 bg-card px-4 py-3">
                      <div className="min-w-0">
                        <p className="font-semibold text-ink-900">{echantillon.reference} · {echantillon.nature}</p>
                        <p className="text-xs text-muted-foreground">
                          {echantillon.demandeReference} · {formaterDate(echantillon.dateReception, true)}
                          {echantillon.receptionneur ? ` · ${echantillon.receptionneur}` : ""}
                        </p>
                      </div>
                      <Badge size="sm" variant={echantillon.conforme ? "mint" : "dark"}>
                        {echantillon.conforme ? "Conforme" : "Non conforme"}
                      </Badge>
                    </li>
                  ))}
                </ul>
              )}
            </section>
          </div>
        </div>
      )}
    </Container>
  );
}

function Indicateur({ icone: Icone, libelle, valeur, lien, alerte }: {
  icone: LucideIcon;
  libelle: string;
  valeur: number;
  lien?: boolean;
  alerte?: boolean;
}) {
  const contenu = (
    <Card className={`flex h-full items-center gap-4 p-4 transition-colors ${lien ? "hover:bg-surface-alt" : ""} ${alerte && valeur > 0 ? "border-red-200" : ""}`}>
      <span className={`flex size-10 items-center justify-center rounded-xl ${alerte && valeur > 0 ? "bg-red-50 text-red-700" : "bg-brand-900/10 text-brand-900"}`}>
        <Icone className="size-5" aria-hidden="true" />
      </span>
      <div>
        <p className="text-2xl font-extrabold text-ink-900">{valeur}</p>
        <p className="text-sm text-muted-foreground">{libelle}</p>
      </div>
    </Card>
  );
  return lien ? (
    <Link to="/app/reception" className="rounded-2xl focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-900">
      {contenu}
    </Link>
  ) : contenu;
}
