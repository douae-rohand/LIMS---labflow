import { createFileRoute, redirect } from "@tanstack/react-router";
import { PageCarte } from "@/components/laboratoires/PageCarte";
import { PublicLayout } from "@/layouts/PublicLayout";
import { redirectionClientConnecte } from "@/lib/redirect";

export const Route = createFileRoute("/carte")({
  head: () => ({
    meta: [
      { title: "Carte des laboratoires — LabFlow LIMS" },
      {
        name: "description",
        content: "Localisez les laboratoires actifs LabFlow au Maroc et calculez un itinéraire.",
      },
      { property: "og:title", content: "Carte des laboratoires — LabFlow LIMS" },
      { property: "og:type", content: "website" },
    ],
  }),
  beforeLoad: ({ location }) => {
    const cible = redirectionClientConnecte(location.href);
    if (cible !== null) throw redirect({ href: cible });
  },
  component: CartePage,
});

function CartePage() {
  return (
    <PublicLayout>
      <PageCarte base="/laboratoires" />
    </PublicLayout>
  );
}
