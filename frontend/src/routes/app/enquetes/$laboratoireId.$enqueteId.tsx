import { Link, createFileRoute } from "@tanstack/react-router";
import { ArrowLeft } from "lucide-react";
import { Button, Card, Skeleton } from "@/components/lab";
import { Container } from "@/components/layout/Container";
import { messageErreurEnquete } from "@/api/enquetesClient";
import { EnqueteDetail } from "@/components/enquetes/EnqueteDetail";
import { useEnqueteClient, useRepondreEnquete } from "@/hooks/useEnquetesClient";

export const Route = createFileRoute("/app/enquetes/$laboratoireId/$enqueteId")({
  head: () => ({
    meta: [{ title: "Détail de l'enquête — LabFlow LIMS" }],
  }),
  component: DetailEnquetePage,
});

function DetailEnquetePage() {
  const { laboratoireId, enqueteId } = Route.useParams();
  const idLaboratoire = Number(laboratoireId);
  const idEnquete = Number(enqueteId);
  const paramsValides =
    Number.isInteger(idLaboratoire) &&
    idLaboratoire > 0 &&
    Number.isInteger(idEnquete) &&
    idEnquete > 0;

  const detailQuery = useEnqueteClient(
    paramsValides ? idLaboratoire : undefined,
    paramsValides ? idEnquete : undefined,
  );
  const reponse = useRepondreEnquete();

  const retour = (
    <Link
      to="/app/enquetes"
      className="inline-flex items-center gap-1.5 rounded-md text-sm font-semibold text-ink-900 transition-colors hover:text-brand-900 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-900"
    >
      <ArrowLeft className="size-4" aria-hidden="true" />
      Mes enquêtes
    </Link>
  );

  if (!paramsValides) {
    return (
      <Container className="py-10">
        {retour}
        <Card variant="surface" className="mt-6" role="alert">
          <p className="text-sm font-semibold text-ink-900">Enquête introuvable.</p>
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
            {messageErreurEnquete(detailQuery.error)}
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
      <EnqueteDetail
        enquete={detailQuery.data}
        enregistrement={reponse.isPending}
        onRepondre={(body) =>
          reponse.mutate({ laboratoireId: idLaboratoire, enqueteId: idEnquete, body })
        }
      />
    </Container>
  );
}
