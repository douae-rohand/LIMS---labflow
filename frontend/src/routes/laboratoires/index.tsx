import { createFileRoute, redirect } from "@tanstack/react-router";
import { PageAnnuaire } from "@/components/laboratoires/PageAnnuaire";
import { PublicLayout } from "@/layouts/PublicLayout";
import { redirectionClientConnecte } from "@/lib/redirect";

export const Route = createFileRoute("/laboratoires/")({
  head: () => ({
    meta: [
      { title: "Laboratoires — LabFlow LIMS" },
      {
        name: "description",
        content: "Consultez les laboratoires actifs LabFlow et leurs analyses.",
      },
      { property: "og:title", content: "Laboratoires — LabFlow LIMS" },
      { property: "og:type", content: "website" },
    ],
  }),
  beforeLoad: ({ location }) => {
    const cible = redirectionClientConnecte(location.href);
    if (cible !== null) throw redirect({ href: cible });
  },
  component: LaboratoiresPage,
});

function LaboratoiresPage() {
  return (
    <PublicLayout>
      <PageAnnuaire base="/laboratoires" />
    </PublicLayout>
  );
}
