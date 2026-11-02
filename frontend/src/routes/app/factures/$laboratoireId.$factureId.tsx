import { Link, createFileRoute } from "@tanstack/react-router";
import { ArrowLeft } from "lucide-react";
import { Button, Card, Skeleton } from "@/components/lab";
import { Container } from "@/components/layout/Container";
import { messageErreurFacture } from "@/api/facturesClient";
import { FactureDetail } from "@/components/factures/FactureDetail";
import { useFactureClient } from "@/hooks/useFacturesClient";

export const Route = createFileRoute("/app/factures/$laboratoireId/$factureId")({
  head: () => ({
    meta: [{ title: "Détail de la facture — LabFlow LIMS" }],
  }),
  component: DetailFacturePage,
});

function DetailFacturePage() {
  const { laboratoireId, factureId } = Route.useParams();
  const idLaboratoire = Number(laboratoireId);
  const idFacture = Number(factureId);
  const paramsValides =
    Number.isInteger(idLaboratoire) &&
    idLaboratoire > 0 &&
    Number.isInteger(idFacture) &&
    idFacture > 0;

  const detailQuery = useFactureClient(
    paramsValides ? idLaboratoire : undefined,
    paramsValides ? idFacture : undefined,
  );

  const retour = (
    <Link
      to="/app/factures"
      className="inline-flex items-center gap-1.5 rounded-md text-sm font-semibold text-ink-900 transition-colors hover:text-brand-900 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-900"
    >
      <ArrowLeft className="size-4" aria-hidden="true" />
      Mes factures
    </Link>
  );

  if (!paramsValides) {
    return (
      <Container className="py-10">
        {retour}
        <Card variant="surface" className="mt-6" role="alert">
          <p className="text-sm font-semibold text-ink-900">Facture introuvable.</p>
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
            {messageErreurFacture(detailQuery.error)}
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
      <FactureDetail facture={detailQuery.data} />
    </Container>
  );
}
