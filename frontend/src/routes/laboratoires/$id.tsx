import { createFileRoute, redirect } from "@tanstack/react-router";
import { PageLaboratoire } from "@/components/laboratoires/PageLaboratoire";
import { PublicLayout } from "@/layouts/PublicLayout";
import { redirectionClientConnecte } from "@/lib/redirect";

export const Route = createFileRoute("/laboratoires/$id")({
  validateSearch: (search: Record<string, unknown>): { ajouter?: number } => {
    const valeur = Number(search["ajouter"]);
    return Number.isInteger(valeur) && valeur > 0 ? { ajouter: valeur } : {};
  },
  head: ({ params }) => ({
    meta: [
      { title: `Laboratoire ${params.id} — LabFlow LIMS` },
      {
        name: "description",
        content: "Fiche d'un laboratoire LabFlow : contact, localisation et analyses proposées.",
      },
    ],
  }),
  beforeLoad: ({ location }) => {
    const cible = redirectionClientConnecte(location.href);
    if (cible !== null) throw redirect({ href: cible });
  },
  component: LaboratoireDetailPage,
});

function LaboratoireDetailPage() {
  const { id } = Route.useParams();
  const search = Route.useSearch();

  return (
    <PublicLayout>
      <PageLaboratoire id={id} ajouter={search.ajouter} base="/laboratoires" />
    </PublicLayout>
  );
}
