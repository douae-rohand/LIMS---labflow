import { createFileRoute } from "@tanstack/react-router";
import { exigerSessionClient } from "@/lib/gardeClient";
import { ProfilClient } from "@/components/profil/ProfilClient";

/** Espace client « Mon profil » : réservé au rôle CLIENT. */
export const Route = createFileRoute("/app/profil")({
  head: () => ({
    meta: [{ title: "Mon profil — LabFlow LIMS" }],
  }),
  beforeLoad: ({ location }) => {
    exigerSessionClient(location);
  },
  component: ProfilClient,
});
