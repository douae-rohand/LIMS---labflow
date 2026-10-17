import { createFileRoute } from "@tanstack/react-router";
import { PageAnnuaire } from "@/components/laboratoires/PageAnnuaire";

export const Route = createFileRoute("/app/laboratoires/")({
  head: () => ({
    meta: [
      { title: "Laboratoires — LabFlow LIMS" },
      {
        name: "description",
        content: "Parcourez les laboratoires actifs et leurs analyses depuis votre espace client.",
      },
    ],
  }),
  component: LaboratoiresPage,
});

function LaboratoiresPage() {
  return <PageAnnuaire base="/app/laboratoires" />;
}
