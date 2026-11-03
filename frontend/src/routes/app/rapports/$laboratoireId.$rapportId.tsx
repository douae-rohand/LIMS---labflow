import { Link, createFileRoute } from "@tanstack/react-router";
import { ArrowLeft } from "lucide-react";
import { Button, Card, Skeleton } from "@/components/lab";
import { Container } from "@/components/layout/Container";
import { messageErreurRapport } from "@/api/rapportsClient";
import { RapportDetail } from "@/components/rapports/RapportDetail";
import { useRapportClient } from "@/hooks/useRapportsClient";

export const Route = createFileRoute("/app/rapports/$laboratoireId/$rapportId")({
  head: () => ({
    meta: [{ title: "Détail du rapport — LabFlow LIMS" }],
  }),
  component: DetailRapportPage,
});

function DetailRapportPage() {
  const { laboratoireId, rapportId } = Route.useParams();
  const idLaboratoire = Number(laboratoireId);
  const idRapport = Number(rapportId);
  const paramsValides =
    Number.isInteger(idLaboratoire) &&
    idLaboratoire > 0 &&
    Number.isInteger(idRapport) &&
    idRapport > 0;

  const detailQuery = useRapportClient(
    paramsValides ? idLaboratoire : undefined,
    paramsValides ? idRapport : undefined,
  );

  const retour = (
    <Link
      to="/app/rapports"
      className="inline-flex items-center gap-1.5 rounded-md text-sm font-semibold text-ink-900 transition-colors hover:text-brand-900 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-900"
    >
      <ArrowLeft className="size-4" aria-hidden="true" />
      Mes rapports
    </Link>
  );

  if (!paramsValides) {
    return (
      <Container className="py-10">
        {retour}
        <Card variant="surface" className="mt-6" role="alert">
          <p className="text-sm font-semibold text-ink-900">Rapport introuvable.</p>
        </Card>
      </Container>
    );
  }

  if (detailQuery.isPending) {
    return (
      <Container className="py-10">
        <div aria-busy="true" className="flex flex-col gap-4">
          <Skeleton className="h-40 w-full" />
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
            {messageErreurRapport(detailQuery.error)}
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

  return (
    <Container className="py-10">
      <div className="mb-6">{retour}</div>
      <RapportDetail rapport={detailQuery.data} />
    </Container>
  );
}
