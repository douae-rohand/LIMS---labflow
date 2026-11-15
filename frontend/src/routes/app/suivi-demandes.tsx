import { createFileRoute, redirect } from "@tanstack/react-router";
import { getSession } from "@/api/session";
import { exigerPermission } from "@/features/shell/garde";
import { PageSuiviDemandes } from "@/features/accueil/PageSuiviDemandes";

export const Route = createFileRoute("/app/suivi-demandes")({
  head: () => ({
    meta: [{ title: "Suivi des demandes — LabFlow LIMS" }],
  }),
  beforeLoad: () => {
    if (getSession()?.user.role !== "ACCUEIL") {
      throw redirect({ to: "/app" });
    }
    exigerPermission("DEMANDES_SUIVI");
  },
  component: PageSuiviDemandes,
});
