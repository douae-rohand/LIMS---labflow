import { createFileRoute, redirect } from "@tanstack/react-router";
import { getSession } from "@/api/session";
import { PageNouvelleDemande } from "@/features/accueil/PageNouvelleDemande";

export const Route = createFileRoute("/app/nouvelle-demande")({
  head: () => ({
    meta: [{ title: "Nouvelle demande — LabFlow LIMS" }],
  }),
  beforeLoad: () => {
    if (getSession()?.user.role !== "ACCUEIL") {
      throw redirect({ to: "/app" });
    }
  },
  component: PageNouvelleDemande,
});
