import { createFileRoute, redirect } from "@tanstack/react-router";
import { getSession } from "@/api/session";
import { PageClients } from "@/features/accueil/PageClients";

export const Route = createFileRoute("/app/clients-accueil")({
  head: () => ({
    meta: [{ title: "Clients et patients — LabFlow LIMS" }],
  }),
  beforeLoad: () => {
    if (getSession()?.user.role !== "ACCUEIL") {
      throw redirect({ to: "/app" });
    }
  },
  component: PageClients,
});
