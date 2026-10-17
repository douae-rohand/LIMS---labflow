import { Link, createFileRoute } from "@tanstack/react-router";
import { useState } from "react";
import { ArrowLeft } from "lucide-react";
import { Button, Card, Skeleton } from "@/components/lab";
import { Container } from "@/components/layout/Container";
import { messageErreurDetailClient } from "@/api/demandesClient";
import { AnnulerDemandeDialog } from "@/components/demandes/client/AnnulerDemandeDialog";
import { DetailDemandeEntete } from "@/components/demandes/client/DetailDemandeEntete";
import { DetailDemandeEssais } from "@/components/demandes/client/DetailDemandeEssais";
import { EchantillonsRecus } from "@/components/demandes/client/EchantillonsRecus";
import { FriseStatuts } from "@/components/demandes/client/FriseStatuts";
import { HistoriqueDemande } from "@/components/demandes/client/HistoriqueDemande";
import { dateArretDemande } from "@/components/demandes/client/libelles";
import { useAnnulerDemande, useDemandeClient } from "@/hooks/useDemandesClient";

export const Route = createFileRoute("/app/demandes/$laboratoireId/$demandeId")({
  head: () => ({
    meta: [{ title: "Détail de la demande — LabFlow LIMS" }],
  }),
  component: DetailDemandePage,
});

function DetailDemandePage() {
  const { laboratoireId, demandeId } = Route.useParams();
  const idLaboratoire = Number(laboratoireId);
  const idDemande = Number(demandeId);
  const paramsValides =
    Number.isInteger(idLaboratoire) &&
    idLaboratoire > 0 &&
    Number.isInteger(idDemande) &&
    idDemande > 0;

  const detailQuery = useDemandeClient(
    paramsValides ? idLaboratoire : undefined,
    paramsValides ? idDemande : undefined,
  );
  const annulation = useAnnulerDemande();
  const [dialogOuvert, setDialogOuvert] = useState(false);

  const retour = (
    <Link to="/app/demandes" className="inline-flex">
      <Button
        variant="ghost"
        size="sm"
        icon={<ArrowLeft className="size-4" aria-hidden="true" />}
        iconPosition="left"
      >
        Mes demandes
      </Button>
    </Link>
  );

  if (!paramsValides) {
    return (
      <Container className="py-10">
        {retour}
        <Card variant="surface" className="mt-6" role="alert">
          <p className="text-sm font-semibold text-ink-900">Demande introuvable.</p>
        </Card>
      </Container>
    );
  }

  if (detailQuery.isPending) {
    return (
      <Container className="py-10">
        <div aria-busy="true" className="flex flex-col gap-4">
          <Skeleton className="h-40 w-full" />
          <Skeleton className="h-24 w-full" />
          <Skeleton className="h-56 w-full" />
        </div>
      </Container>
    );
  }

  if (detailQuery.isError || detailQuery.data === undefined) {
    return (
      <Container className="py-10">
        {retour}
        <Card variant="surface" className="mt-6" role="alert">
          <p className="text-sm font-semibold text-ink-900">
            {messageErreurDetailClient(detailQuery.error)}
          </p>
          <div className="mt-4">
            <Button variant="secondary" size="sm" onClick={() => void detailQuery.refetch()}>
              Réessayer
            </Button>
          </div>
        </Card>
      </Container>
    );
  }

  const demande = detailQuery.data;

  return (
    <Container className="py-10">
      <div className="mb-6">{retour}</div>

      <div className="flex flex-col gap-8">
        <DetailDemandeEntete
          demande={demande}
          onDemanderAnnulation={() => setDialogOuvert(true)}
        />

        <section>
          <h2 className="mb-4 text-lg font-extrabold text-ink-900">Avancement</h2>
          <FriseStatuts
            statut={demande.statut}
            dateArret={dateArretDemande(demande.historique)}
          />
        </section>

        <section>
          <h2 className="mb-4 text-lg font-extrabold text-ink-900">Essais demandés</h2>
          <DetailDemandeEssais lignes={demande.lignes} total={demande.total} />
        </section>

        <section>
          <h2 className="mb-4 text-lg font-extrabold text-ink-900">Échantillons reçus</h2>
          <EchantillonsRecus echantillons={demande.echantillons} />
        </section>

        <section>
          <h2 className="mb-4 text-lg font-extrabold text-ink-900">Historique</h2>
          <HistoriqueDemande historique={demande.historique} />
        </section>
      </div>

      <AnnulerDemandeDialog
        open={dialogOuvert}
        chargement={annulation.isPending}
        onOpenChange={setDialogOuvert}
        onConfirmer={(motif) => {
          annulation.mutate(
            { laboratoireId: idLaboratoire, demandeId: idDemande, motif },
            { onSuccess: () => setDialogOuvert(false) },
          );
        }}
      />
    </Container>
  );
}
