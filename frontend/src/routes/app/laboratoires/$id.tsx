import { createFileRoute } from "@tanstack/react-router";
import { PageLaboratoire } from "@/components/laboratoires/PageLaboratoire";

export const Route = createFileRoute("/app/laboratoires/$id")({
  validateSearch: (search: Record<string, unknown>): { ajouter?: number } => {
    const valeur = Number(search["ajouter"]);
    return Number.isInteger(valeur) && valeur > 0 ? { ajouter: valeur } : {};
  },
  head: ({ params }) => ({
    meta: [
      { title: `Laboratoire ${params.id} — LabFlow LIMS` },
      {
        name: "description",
        content: "Fiche d'un laboratoire et de ses analyses depuis votre espace client.",
      },
    ],
  }),
  component: LaboratoireDetailPage,
});

function LaboratoireDetailPage() {
  const { id } = Route.useParams();
  const search = Route.useSearch();

  return <PageLaboratoire id={id} ajouter={search.ajouter} base="/app/laboratoires" />;
}
