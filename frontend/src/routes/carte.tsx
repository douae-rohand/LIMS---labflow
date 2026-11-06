import { createFileRoute, redirect } from "@tanstack/react-router";
import { PageCarte } from "@/components/laboratoires/PageCarte";
import { PublicLayout } from "@/layouts/PublicLayout";
import { getSession } from "@/api/session";

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
  beforeLoad: () => {
    const session = getSession();
    if (session && session.user.role === "CLIENT") {
      if (session.user.mustChangePassword) {
        throw redirect({ to: "/changer-mot-de-passe" });
      }
      throw redirect({ to: "/app/carte" });
    }
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
