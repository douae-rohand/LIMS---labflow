import { createFileRoute, redirect } from "@tanstack/react-router";
import { PageAnnuaire } from "@/components/laboratoires/PageAnnuaire";
import { PublicLayout } from "@/layouts/PublicLayout";
import { getSession } from "@/api/session";

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
  beforeLoad: () => {
    const session = getSession();
    if (session && session.user.role === "CLIENT") {
      if (session.user.mustChangePassword) {
        throw redirect({ to: "/changer-mot-de-passe" });
      }
      throw redirect({ to: "/app/laboratoires" });
    }
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
